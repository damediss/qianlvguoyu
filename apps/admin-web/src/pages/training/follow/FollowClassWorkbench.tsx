import { useEffect, useMemo, useState } from 'react';
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
  Popconfirm,
  Progress,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tabs,
  Tag,
  TimePicker,
  Typography,
  Upload,
  message,
} from 'antd';
import {
  ArrowLeftOutlined,
  CheckCircleOutlined,
  DeleteOutlined,
  EditOutlined,
  ExportOutlined,
  HistoryOutlined,
  LineChartOutlined,
  PlusOutlined,
  ReloadOutlined,
  StarOutlined,
  TeamOutlined,
  UploadOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs, { type Dayjs } from 'dayjs';
import { downloadUrlFile } from '../../../api/files';
import * as trainApi from '../../../api/training';
import type { ClassStudent, TrainInfoClassListItem } from '../../../types/training';
import { StudentAttendanceDetailDrawer } from './StudentAttendanceDetailDrawer';
import { FollowPracticeTab } from './FollowPracticeTab';
import { FollowScheduleChangeTab } from './FollowScheduleChangeTab';
import { FollowHomeworkTab } from './FollowHomeworkTab';
import { FollowMockExamTab } from './FollowMockExamTab';
import { FollowEvidenceTab } from './FollowEvidenceTab';
import { FollowLogsSupervisionTab } from './FollowLogsSupervisionTab';
import { ClassRolesPanel } from '../ClassRolesPanel';

type ScheduleDraftItem = Record<string, unknown> & {
  _key: string;
  id?: number | string;
  scheduleDate?: string;
  dayOfWeek?: number;
  courseName?: string;
  classHours?: string;
  teachForm?: string;
  teacherName?: string;
  roomName?: string;
  startTime?: string;
  endTime?: string;
};

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
  const [bizDate, setBizDate] = useState<Dayjs | null>(dayjs());
  const [sessionKey, setSessionKey] = useState<string>('day');
  const [studentPage, setStudentPage] = useState(1);
  const [detailStudent, setDetailStudent] = useState<ClassStudent | null>(null);
  const [importResult, setImportResult] = useState<trainApi.TrainAttendanceImportResult | null>(null);
  const [scheduleDraft, setScheduleDraft] = useState<ScheduleDraftItem[]>([]);
  const [scheduleDirty, setScheduleDirty] = useState(false);
  const [scheduleModalOpen, setScheduleModalOpen] = useState(false);
  const [editingScheduleKey, setEditingScheduleKey] = useState<string | null>(null);
  const [scheduleForm] = Form.useForm();

  const dateStr = bizDate ? bizDate.format('YYYY-MM-DD') : '';
  const hasBizDate = Boolean(dateStr);
  /** 课次跟班：sessionKey 为课次 id；day=按日（schedule_id 空） */
  const selectedScheduleId = useMemo(() => {
    if (sessionKey === 'day') return null;
    const n = Number(sessionKey);
    return Number.isFinite(n) && n > 0 ? n : null;
  }, [sessionKey]);
  const isSessionAttendance = selectedScheduleId != null;
  const { data: scheduleStatus, refetch: refetchScheduleStatus } = useQuery({
    queryKey: ['follow-schedule-status', classIdNum],
    queryFn: () => trainApi.getClassScheduleStatus(classIdNum!),
    enabled: classIdNum != null && classIdNum > 0,
  });

  const hasKcbiaoFile = Boolean(
    scheduleStatus?.hasKcbiaoFile || classItem.kcbiao_file || classItem.kcbiao_src,
  );
  const scheduleManualEdited = Boolean(scheduleStatus?.manualEdited);

  const { data: scheduleData, isLoading: scheduleLoading } = useQuery({
    queryKey: ['follow-schedule', classIdNum],
    queryFn: () => trainApi.getClassSchedule(classIdNum!),
    enabled: classIdNum != null && classIdNum > 0,
  });

  const { data: pendingChangeRequests } = useQuery({
    queryKey: ['schedule-change-requests', classIdNum, 'pending'],
    queryFn: () => trainApi.listScheduleChangeRequests({ classId: classIdNum!, status: 'pending' }),
    enabled: classIdNum != null && classIdNum > 0,
  });
  const pendingChangeCount = pendingChangeRequests?.length ?? 0;

  const { data: practiceScope, isLoading: practiceScopeLoading } = useQuery({
    queryKey: ['class-practice-scope', classId],
    queryFn: () => trainApi.getClassPracticeScope(classId),
    enabled: !!classId,
  });

  useEffect(() => {
    if (scheduleDirty) return;
    const list = ((scheduleData ?? []) as ScheduleDraftItem[]).map((row, idx) => ({
      ...row,
      _key: String(row.id ?? `tmp-${idx}-${row.scheduleDate}-${row.startTime}`),
    }));
    list.sort((a, b) => {
      const da = String(a.scheduleDate ?? '');
      const db = String(b.scheduleDate ?? '');
      if (da !== db) return da.localeCompare(db);
      return String(a.startTime ?? '').localeCompare(String(b.startTime ?? ''));
    });
    setScheduleDraft(list as ScheduleDraftItem[]);
  }, [scheduleData, scheduleDirty]);

  const scheduleList = scheduleDraft;

  const scheduleItems = useMemo(() => {
    return scheduleList.filter((s) => String(s.scheduleDate ?? '') === dateStr);
  }, [scheduleList, dateStr]);

  /** 概览课表摘要：节次 / 学时合计 / 科目数 / 授课形式 / 上课天数 */
  const scheduleStats = useMemo(() => {
    const subjects = new Set<string>();
    const teachers = new Set<string>();
    const days = new Set<string>();
    let totalHours = 0;
    let hoursKnown = 0;
    let theoryCount = 0;
    let practiceCount = 0;
    for (const row of scheduleList) {
      const name = String(row.courseName ?? '').trim();
      if (name) subjects.add(name);
      const teacher = String(row.teacherName ?? '').trim();
      if (teacher) teachers.add(teacher);
      const day = String(row.scheduleDate ?? '').trim();
      if (day) days.add(day);
      const hoursRaw = String(row.classHours ?? '').trim();
      if (hoursRaw) {
        const n = Number.parseFloat(hoursRaw.replace(/[^\d.-]/g, ''));
        if (Number.isFinite(n)) {
          totalHours += n;
          hoursKnown += 1;
        }
      }
      const form = String(row.teachForm ?? '').trim();
      if (form.includes('实操') || form.includes('实作') || form.includes('操作')) {
        practiceCount += 1;
      } else if (form.includes('理论')) {
        theoryCount += 1;
      }
    }
    const roundHours = Math.round(totalHours * 10) / 10;
    return {
      sessionCount: scheduleList.length,
      subjectCount: subjects.size,
      teacherCount: teachers.size,
      dayCount: days.size,
      totalHours: roundHours,
      hoursKnown,
      theoryCount,
      practiceCount,
      todayCount: scheduleItems.length,
    };
  }, [scheduleList, scheduleItems.length]);

  const dayOfWeekLabel = (v: unknown) => {
    const n = typeof v === 'number' ? v : Number(v);
    return ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日'][n] ?? '';
  };

  const saveScheduleMu = useMutation({
    mutationFn: () => {
      if (classIdNum == null) return Promise.reject(new Error('无效班级'));
      const items = scheduleDraft.map(({ _key, ...rest }) => {
        void _key;
        return rest;
      });
      return trainApi.saveClassSchedule(classIdNum, items);
    },
    onSuccess: () => {
      message.success('课表已保存');
      setScheduleDirty(false);
      queryClient.invalidateQueries({ queryKey: ['follow-schedule', classIdNum] });
      void refetchScheduleStatus();
    },
    onError: (e: Error) => message.error(e.message || '保存课表失败'),
  });

  const importKcbiaoMu = useMutation({
    mutationFn: (opts: { force: boolean; confirmOverwriteManual?: boolean }) => {
      if (classIdNum == null) return Promise.reject(new Error('无效班级'));
      return trainApi.importClassScheduleFromKcbiao(
        classIdNum,
        opts.force,
        opts.confirmOverwriteManual ?? false,
      );
    },
    onSuccess: (res) => {
      if (res.skipped) {
        message.info(res.reason || '已有课表，未覆盖');
      } else {
        message.success(`已从开班课表导入 ${res.imported ?? res.count ?? 0} 节`);
      }
      setScheduleDirty(false);
      queryClient.invalidateQueries({ queryKey: ['follow-schedule', classIdNum] });
      void refetchScheduleStatus();
    },
    onError: (e: Error) => message.error(e.message || '导入开班课表失败'),
  });

  const exportScheduleMu = useMutation({
    mutationFn: () => {
      if (classIdNum == null) return Promise.reject(new Error('无效班级'));
      return trainApi.exportClassSchedule(classIdNum);
    },
    onSuccess: async (data) => {
      if (data.downUrl) {
        await downloadUrlFile(data.downUrl, data.fileName || '课表.csv');
      }
      message.success('课表已导出');
    },
    onError: (e: Error) => message.error(e.message || '导出课表失败'),
  });

  const runExportSchedule = () => {
    if (scheduleList.length === 0) {
      message.warning('暂无课次可导出');
      return;
    }
    if (scheduleDirty) {
      Modal.confirm({
        title: '导出已保存课表？',
        content: '当前有未保存的课表修改，导出内容为服务端已保存版本，不含本次未保存改动。',
        okText: '继续导出',
        onOk: () => exportScheduleMu.mutateAsync(),
      });
      return;
    }
    exportScheduleMu.mutate();
  };

  const runImportKcbiao = (force: boolean) => {
    if (force && scheduleManualEdited) {
      Modal.confirm({
        title: '确认覆盖已调课课表？',
        content:
          '本班课表已在跟班手工调整过。用开班 Excel 重导将覆盖当前结构化课次，开班原文件不会被改写。',
        okText: '确认覆盖',
        okButtonProps: { danger: true },
        onOk: () => importKcbiaoMu.mutateAsync({ force: true, confirmOverwriteManual: true }),
      });
      return;
    }
    importKcbiaoMu.mutate({ force, confirmOverwriteManual: false });
  };

  const openScheduleEditor = (row?: ScheduleDraftItem) => {
    if (row) {
      setEditingScheduleKey(row._key);
      scheduleForm.setFieldsValue({
        scheduleDate: row.scheduleDate ? dayjs(String(row.scheduleDate)) : undefined,
        courseName: row.courseName,
        classHours: row.classHours,
        teachForm: row.teachForm,
        teacherName: row.teacherName,
        timeRange:
          row.startTime && row.endTime
            ? [dayjs(String(row.startTime), 'HH:mm'), dayjs(String(row.endTime), 'HH:mm')]
            : undefined,
      });
    } else {
      setEditingScheduleKey(null);
      scheduleForm.setFieldsValue({
        scheduleDate: bizDate ?? undefined,
        courseName: undefined,
        classHours: undefined,
        teachForm: undefined,
        teacherName: undefined,
        timeRange: undefined,
      });
    }
    setScheduleModalOpen(true);
  };

  const submitScheduleEditor = async () => {
    const v = await scheduleForm.validateFields();
    const scheduleDate = (v.scheduleDate as Dayjs).format('YYYY-MM-DD');
    const startTime = (v.timeRange[0] as Dayjs).format('HH:mm');
    const endTime = (v.timeRange[1] as Dayjs).format('HH:mm');
    const dayOfWeek = dayjs(scheduleDate).day() === 0 ? 7 : dayjs(scheduleDate).day();
    const next: ScheduleDraftItem = {
      ...(editingScheduleKey
        ? scheduleDraft.find((x) => x._key === editingScheduleKey) ?? { _key: editingScheduleKey }
        : { _key: `new-${Date.now()}` }),
      scheduleDate,
      dayOfWeek,
      courseName: String(v.courseName).trim(),
      classHours: v.classHours != null && String(v.classHours).trim() ? String(v.classHours).trim() : undefined,
      teachForm: v.teachForm != null && String(v.teachForm).trim() ? String(v.teachForm).trim() : undefined,
      teacherName: v.teacherName ? String(v.teacherName).trim() : undefined,
      startTime,
      endTime,
      status: 0,
    };
    setScheduleDraft((prev) => {
      if (editingScheduleKey) {
        return prev.map((x) => (x._key === editingScheduleKey ? next : x));
      }
      return [...prev, next];
    });
    setScheduleDirty(true);
    setScheduleModalOpen(false);
  };

  const removeScheduleRow = (key: string) => {
    setScheduleDraft((prev) => prev.filter((x) => x._key !== key));
    setScheduleDirty(true);
  };

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

  const { data: roleSummary } = useQuery({
    queryKey: ['follow-role-summary', classId],
    queryFn: () => trainApi.getClassRoleSummary(classId),
    enabled: !!classId,
  });
  const classRoles = roleSummary?.roles ?? [];

  const { data: attendancePage, isLoading: attendanceLoading, isError: attendanceError } = useQuery({
    queryKey: ['follow-attendance', classIdNum, dateStr, sessionKey],
    queryFn: () =>
      trainApi.pageAttendances({
        page: 1,
        pageSize: 500,
        classId: classIdNum!,
        attendanceDate: dateStr,
        ...(isSessionAttendance ? { scheduleId: selectedScheduleId! } : {}),
      }),
    enabled: classIdNum != null && classIdNum > 0 && hasBizDate,
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

  const { data: homeworkMetrics, isLoading: homeworkMetricsLoading } = useQuery({
    queryKey: ['homework-metrics', classIdNum],
    queryFn: () => trainApi.getHomeworkMetrics(classIdNum!),
    enabled: classIdNum != null && classIdNum > 0,
    retry: false,
  });

  const { data: mockMetrics, isLoading: mockMetricsLoading } = useQuery({
    queryKey: ['mock-metrics', classIdNum],
    queryFn: () => trainApi.getMockMetrics(classIdNum!),
    enabled: classIdNum != null && classIdNum > 0,
    retry: false,
  });

  const { data: qualityMetrics, isLoading: qualityMetricsLoading } = useQuery({
    queryKey: ['follow-quality-metrics', classIdNum],
    queryFn: () => trainApi.getFollowQualityMetrics(classIdNum!),
    enabled: classIdNum != null && classIdNum > 0,
    retry: false,
  });

  const attendanceMap = useMemo(() => {
    const map = new Map<string, trainApi.TrainAttendanceItem>();
    const rows = attendancePage?.items ?? [];
    if (isSessionAttendance) {
      for (const row of rows) {
        if (row.userId != null) map.set(String(row.userId), row);
      }
      return map;
    }
    // 按日：优先 schedule_id 空；否则同日多课次「任一有效出勤即出勤」
    const byUser = new Map<string, trainApi.TrainAttendanceItem[]>();
    for (const row of rows) {
      if (row.userId == null) continue;
      const key = String(row.userId);
      const list = byUser.get(key) ?? [];
      list.push(row);
      byUser.set(key, list);
    }
    for (const [uid, list] of byUser) {
      const dayLevel = list.find((r) => r.scheduleId == null || r.scheduleId === undefined);
      if (dayLevel) {
        map.set(uid, dayLevel);
        continue;
      }
      const present = list.find((r) => r.status != null && r.status > 0);
      map.set(uid, present ?? list[0]);
    }
    return map;
  }, [attendancePage?.items, isSessionAttendance]);

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

    const expected = rosterTotal > 0 ? rosterTotal : allStudents.length;
    const dailyTrend = [...byDate.entries()]
      .map(([date, v]) => ({
        date,
        present: v.present,
        absent: v.absent,
        /** 应到：班级总人数 */
        total: expected,
        rate: expected > 0 ? Math.round((v.present / expected) * 1000) / 10 : 0,
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
  }, [allAttendancePage?.items, allStudents, allAttendanceLoading, rosterTotal]);

  const signInMu = useMutation({
    mutationFn: (student: ClassStudent) => {
      if (!hasBizDate) return Promise.reject(new Error('请先选择业务日期'));
      const uid = toNumId(student.id);
      if (uid == null) return Promise.reject(new Error('无效学员ID'));
      const existing = attendanceMap.get(String(student.id));
      // 按日视图若折叠自课次记录，不可直接 update 课次行冒充按日
      const canUpdateExisting =
        existing?.id &&
        (isSessionAttendance
          ? Number(existing.scheduleId) === selectedScheduleId
          : existing.scheduleId == null || existing.scheduleId === undefined);
      if (canUpdateExisting && existing?.id) {
        return trainApi.updateAttendance(existing.id, {
          status: 1,
          checkInTime: dayjs().format('HH:mm:ss'),
          scheduleId: selectedScheduleId,
        });
      }
      return trainApi.signInAttendance({
        classId: classIdNum ?? undefined,
        projectId: toNumId(trainInfoId ?? 0) ?? undefined,
        userId: uid,
        userName: student.realname,
        attendanceDate: dateStr,
        scheduleId: selectedScheduleId,
        status: 1,
        checkInTime: dayjs().format('HH:mm:ss'),
      });
    },
    onSuccess: () => {
      message.success(isSessionAttendance ? '已标记本课次出勤' : '已标记当日出勤');
      queryClient.invalidateQueries({ queryKey: ['follow-attendance', classIdNum] });
      queryClient.invalidateQueries({ queryKey: ['follow-attendance-all', classIdNum] });
      queryClient.invalidateQueries({ queryKey: ['follow-student-attendance-detail'] });
      queryClient.invalidateQueries({ queryKey: ['follow-quality-metrics', classIdNum] });
    },
    onError: (e: Error) => message.error(e.message || '考勤操作失败（请确认账号具备 train:attendance 权限）'),
  });

  const markAbsentMu = useMutation({
    mutationFn: (student: ClassStudent) => {
      if (!hasBizDate) return Promise.reject(new Error('请先选择业务日期'));
      const uid = toNumId(student.id);
      if (uid == null) return Promise.reject(new Error('无效学员ID'));
      const existing = attendanceMap.get(String(student.id));
      const canUpdateExisting =
        existing?.id &&
        (isSessionAttendance
          ? Number(existing.scheduleId) === selectedScheduleId
          : existing.scheduleId == null || existing.scheduleId === undefined);
      if (canUpdateExisting && existing?.id) {
        return trainApi.updateAttendance(existing.id, {
          status: 0,
          scheduleId: selectedScheduleId,
        });
      }
      return trainApi.createAttendance({
        classId: classIdNum ?? undefined,
        projectId: toNumId(trainInfoId ?? 0) ?? undefined,
        userId: uid,
        userName: student.realname,
        attendanceDate: dateStr,
        scheduleId: selectedScheduleId,
        status: 0,
      });
    },
    onSuccess: () => {
      message.success(isSessionAttendance ? '已标记本课次缺勤' : '已标记当日缺勤');
      queryClient.invalidateQueries({ queryKey: ['follow-attendance', classIdNum] });
      queryClient.invalidateQueries({ queryKey: ['follow-attendance-all', classIdNum] });
      queryClient.invalidateQueries({ queryKey: ['follow-student-attendance-detail'] });
      queryClient.invalidateQueries({ queryKey: ['follow-quality-metrics', classIdNum] });
    },
    onError: (e: Error) => message.error(e.message || '考勤操作失败'),
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

  const leaders = classRoles.filter((r) => r.roleCode === 'monitor');
  const staffList = classRoles.filter((r) => r.roleCode === 'follow_staff');
  const chargeList = classRoles.filter((r) => r.roleCode === 'head_teacher');
  const leadTeachers = classRoles.filter((r) => r.roleCode === 'lead_teacher' || r.roleCode === 'assistant');

  const workLogCountQuery = useQuery({
    queryKey: ['follow-work-logs', classIdNum, 'overview-count'],
    queryFn: () => trainApi.listFollowWorkLogs(classIdNum!),
    enabled: classIdNum != null && classIdNum > 0,
  });

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
            <DatePicker
              value={bizDate}
              allowClear
              placeholder="选择或清空"
              onChange={(d) => {
                setBizDate(d);
                setSessionKey('day');
              }}
            />
          </span>
          <span>
            课次：
            <Select
              style={{ width: 280 }}
              value={sessionKey}
              onChange={setSessionKey}
              disabled={!hasBizDate}
              options={[
                { value: 'day', label: scheduleItems.length ? '按日（无指定课次）' : '按日跟班（当日无排课）' },
                ...scheduleItems.map((s, idx) => ({
                  value: String(s.id ?? `s-${idx}`),
                  label: `${String(s.courseName || '课程')} ${String(s.startTime || '')}-${String(s.endTime || '')}`,
                })),
              ]}
            />
          </span>
          <Tag>
            {hasBizDate
              ? isSessionAttendance
                ? '课次考勤'
                : scheduleItems.length
                  ? `按日汇总 · 当日 ${scheduleItems.length} 课次`
                  : '按日考勤 · 无排课'
              : '未选业务日'}
          </Tag>
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
                      <Statistic title={isSessionAttendance ? '本课次实到' : '当日实到'} value={presentCount} suffix={`/ ${dayStats.base || '—'}`} />
                    </Card>
                  </Col>
                  <Col xs={12} sm={8} md={4}>
                    <Card size="small">
                      <Statistic title={isSessionAttendance ? '本课次出勤率' : '当日出勤率'} value={attendanceRate} suffix="%" />
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
                    <Card size="small"><Statistic title="跟班日志" value={workLogCountQuery.data?.length ?? 0} prefix={<HistoryOutlined />} /></Card>
                  </Col>
                </Row>

                <Card size="small" title={`${isSessionAttendance ? '本课次' : '当日'}考勤构成（${hasBizDate ? dateStr : '未选日期'}）`} style={{ marginBottom: 16 }}>
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
                    <Card
                      size="small"
                      className="follow-overview-scroll-card"
                      title="近日出勤率"
                      loading={classStats.loading}
                    >
                      {classStats.dailyTrend.length === 0 ? (
                        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无考勤记录，可导入考勤表或当日登记" />
                      ) : (
                        <Table
                          size="small"
                          rowKey="date"
                          pagination={false}
                          sticky
                          dataSource={classStats.dailyTrend}
                          columns={[
                            { title: '日期', dataIndex: 'date', width: 120 },
                            {
                              title: '实到/应到',
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
                      className="follow-overview-scroll-card"
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
                          sticky
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
                
                <Card
                  size="small"
                  title="班级课程表"
                  style={{ marginBottom: 16 }}
                  loading={scheduleLoading}
                  extra={
                    <Button type="link" style={{ padding: 0 }} onClick={() => setActiveTab('schedule')}>
                      管理课表 →
                    </Button>
                  }
                >
                  {scheduleStats.sessionCount === 0 ? (
                    <Empty
                      image={Empty.PRESENTED_IMAGE_SIMPLE}
                      description="暂无课次"
                    >
                      <Button type="primary" size="small" onClick={() => setActiveTab('schedule')}>
                        去配置课程表
                      </Button>
                    </Empty>
                  ) : (
                    <Space direction="vertical" size={12} style={{ width: '100%' }}>
                      <Row gutter={[12, 12]}>
                        <Col xs={12} sm={8} md={4}>
                          <Statistic title="课次" value={scheduleStats.sessionCount} suffix="节" />
                        </Col>
                        <Col xs={12} sm={8} md={4}>
                          <Statistic
                            title="学时合计"
                            value={scheduleStats.totalHours}
                            suffix="课时"
                          />
                        </Col>
                        <Col xs={12} sm={8} md={4}>
                          <Statistic title="科目数" value={scheduleStats.subjectCount} suffix="门" />
                        </Col>
                        <Col xs={12} sm={8} md={4}>
                          <Statistic title="上课天数" value={scheduleStats.dayCount} suffix="天" />
                        </Col>
                        <Col xs={12} sm={8} md={4}>
                          <Statistic title="授课老师" value={scheduleStats.teacherCount} suffix="人" />
                        </Col>
                        <Col xs={12} sm={8} md={4}>
                          <Statistic title="业务日课次" value={scheduleStats.todayCount} suffix="节" />
                        </Col>
                      </Row>
                      <Space wrap size={[8, 4]} style={{ width: '100%', justifyContent: 'space-between' }}>
                        <Space wrap size={8}>
                          {scheduleStats.theoryCount > 0 ? (
                            <Tag color="blue">理论 {scheduleStats.theoryCount} 节</Tag>
                          ) : null}
                          {scheduleStats.practiceCount > 0 ? (
                            <Tag color="green">实操 {scheduleStats.practiceCount} 节</Tag>
                          ) : null}
                          {scheduleStats.hoursKnown < scheduleStats.sessionCount ? (
                            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                              {scheduleStats.hoursKnown === 0
                                ? '学时未录入，合计为 0；可重导开班课表或手工补全'
                                : `有 ${scheduleStats.sessionCount - scheduleStats.hoursKnown} 节未填学时`}
                            </Typography.Text>
                          ) : null}
                          {scheduleDirty ? <Tag color="warning">未保存</Tag> : null}
                          {scheduleManualEdited ? <Tag>已调课</Tag> : null}
                        </Space>
                        <Button type="primary" size="small" onClick={() => setActiveTab('schedule')}>
                          打开课程表
                        </Button>
                      </Space>
                    </Space>
                  )}
                </Card>

                <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
                <Col xs={24} lg={12}>
                    <Card
                      size="small"
                      title="作业完成情况"
                      loading={homeworkMetricsLoading}
                      extra={
                        <Button type="link" style={{ padding: 0 }} onClick={() => setActiveTab('homework')}>
                          作业明细 →
                        </Button>
                      }
                    >
                      <Space size="large" wrap>
                        <Statistic title="已发布" value={homeworkMetrics?.taskCount ?? 0} />
                        <Statistic
                          title="完成率"
                          value={homeworkMetrics?.completionRate ?? 0}
                          suffix="%"
                        />
                        <Statistic
                          title="平均正确率"
                          value={homeworkMetrics?.avgAccuracyRate ?? '—'}
                          suffix={homeworkMetrics?.avgAccuracyRate != null ? '%' : undefined}
                        />
                        <Statistic
                          title="已交/应交"
                          value={`${homeworkMetrics?.gradedItems ?? 0}/${homeworkMetrics?.totalItems ?? 0}`}
                        />
                      </Space>
                    </Card>
                  </Col>
                  <Col xs={24} lg={12}>
                    <Card
                      size="small"
                      title="模拟卷概览"
                      loading={mockMetricsLoading}
                      extra={
                        <Button type="link" style={{ padding: 0 }} onClick={() => setActiveTab('mock')}>
                          模拟考明细 →
                        </Button>
                      }
                    >
                      <Space size="large" wrap>
                        <Statistic title="已发布" value={mockMetrics?.taskCount ?? 0} />
                        <Statistic
                          title="完成率"
                          value={mockMetrics?.completionRate ?? 0}
                          suffix="%"
                        />
                        <Statistic
                          title="平均正确率"
                          value={mockMetrics?.avgAccuracyRate ?? '—'}
                          suffix={mockMetrics?.avgAccuracyRate != null ? '%' : undefined}
                        />
                        <Statistic
                          title="已交/应交"
                          value={`${mockMetrics?.gradedItems ?? 0}/${mockMetrics?.totalItems ?? 0}`}
                        />
                      </Space>
                    </Card>
                  </Col>
                 
                </Row>

                <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
                <Col xs={24} lg={12}>
                    <Card
                      size="small"
                      className="follow-overview-scroll-card"
                      title="作业预警"
                      extra={
                        <Typography.Text type="secondary">连续未交≥2 或平均正确率&lt;60%</Typography.Text>
                      }
                      loading={homeworkMetricsLoading}
                    >
                      {(homeworkMetrics?.warnings?.length ?? 0) === 0 ? (
                        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无作业预警" />
                      ) : (
                        <Table
                          size="small"
                          rowKey={(r) => `${r.peopleId}-${r.type}`}
                          pagination={false}
                          sticky
                          dataSource={homeworkMetrics?.warnings ?? []}
                          columns={[
                            { title: '姓名', dataIndex: 'userName', width: 100 },
                            {
                              title: '类型',
                              dataIndex: 'type',
                              width: 100,
                              render: (t: string) =>
                                t === 'miss' ? <Tag color="warning">未交</Tag> : <Tag color="error">正确率低</Tag>,
                            },
                            { title: '说明', dataIndex: 'message', ellipsis: true },
                          ]}
                        />
                      )}
                    </Card>
                  </Col>
                  <Col xs={24} lg={12}>
                    <Card
                      size="small"
                      className="follow-overview-scroll-card"
                      title="模拟卷预警"
                      extra={
                        <Typography.Text type="secondary">连续未交≥2 或平均正确率&lt;60%</Typography.Text>
                      }
                      loading={mockMetricsLoading}
                    >
                      {(mockMetrics?.warnings?.length ?? 0) === 0 ? (
                        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无模拟卷预警" />
                      ) : (
                        <Table
                          size="small"
                          rowKey={(r) => `mock-${r.peopleId}-${r.type}`}
                          pagination={false}
                          sticky
                          dataSource={mockMetrics?.warnings ?? []}
                          columns={[
                            { title: '姓名', dataIndex: 'userName', width: 100 },
                            {
                              title: '类型',
                              dataIndex: 'type',
                              width: 100,
                              render: (t: string) =>
                                t === 'miss' ? <Tag color="warning">未交</Tag> : <Tag color="error">正确率低</Tag>,
                            },
                            { title: '说明', dataIndex: 'message', ellipsis: true },
                          ]}
                        />
                      )}
                    </Card>
                  </Col>
                
                </Row>

                

                <Card
                  size="small"
                  title="班级刷题试题"
                  style={{ marginBottom: 16 }}
                  loading={practiceScopeLoading}
                  extra={
                    <Button type="link" style={{ padding: 0 }} onClick={() => setActiveTab('practice')}>
                      {practiceScope?.configured ? '查看学情 / 调整配置 →' : '去配置 →'}
                    </Button>
                  }
                >
                  {!practiceScope?.configured || !(practiceScope.courses?.length > 0) ? (
                    <Empty
                      image={Empty.PRESENTED_IMAGE_SIMPLE}
                      description="尚未配置本班可刷科目；学员从 App 班级入口刷题前需先配置"
                    >
                      <Button type="primary" size="small" onClick={() => setActiveTab('practice')}>
                        去刷题 Tab 配置
                      </Button>
                    </Empty>
                  ) : (
                    <Space direction="vertical" size={8} style={{ width: '100%' }}>
                      <div>
                        <Typography.Text type="secondary" style={{ marginRight: 8 }}>
                          科目
                        </Typography.Text>
                        <Space wrap size={[4, 4]}>
                          {practiceScope.courses.map((c) => (
                            <Tag key={c.courseId} color="blue">
                              {c.title || `科目#${c.courseId}`}
                            </Tag>
                          ))}
                        </Space>
                        {practiceScope.enabled === 0 && (
                          <Tag color="default" style={{ marginLeft: 8 }}>
                            已停用
                          </Tag>
                        )}
                      </div>
                      <Typography.Text type="secondary">
                        知识点：
                        {practiceScope.knowledgeIds?.length
                          ? `已限定 ${practiceScope.knowledgeIds.length} 个`
                          : '所选科目下不限知识点'}
                        {' · '}
                        开放日期：
                        {practiceScope.practiceStartTime || practiceScope.practiceEndTime
                          ? `${practiceScope.practiceStartTime || '不限'} ~ ${practiceScope.practiceEndTime || '不过期'}`
                          : '不限（不过期）'}
                      </Typography.Text>
                      {practiceScope.remark ? (
                        <Typography.Text type="secondary">备注：{practiceScope.remark}</Typography.Text>
                      ) : null}
                    </Space>
                  )}
                </Card>

                <Row gutter={[12, 12]}>
                  <Col xs={24} md={12}>
                    <Card size="small" title="角色摘要">
                      <Space direction="vertical" style={{ width: '100%' }}>
                        <div>班长：{leaders.map((s) => s.displayName).join('、') || '未设置'}</div>
                        <div>跟班教务：{staffList.map((s) => s.displayName).join('、') || '未设置'}</div>
                        <div>班主任：{chargeList.map((s) => s.displayName).join('、') || '未设置'}</div>
                        <div>主讲/助教：{leadTeachers.map((s) => s.displayName).join('、') || '未设置'}</div>
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
                        <Button type="link" style={{ padding: 0 }} onClick={() => setActiveTab('schedule')}>去课程表 →</Button>
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
                    支持人社/培训平台「考勤信息列表」.xls/.xlsx；按身份证匹配本班学员，同日多课次合并为「按日」记录（schedule 空）。
                    顶部选择具体课次后，出勤/缺勤写入该课次。
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
            key: 'schedule',
            label: '课程表',
            children: (
              <div>
                <Card
                  size="small"
                  title="班级课程表"
                  style={{ marginBottom: 16 }}
                  loading={scheduleLoading || importKcbiaoMu.isPending || saveScheduleMu.isPending}
                  extra={
                    <Space wrap size="small">
                      <Typography.Text type="secondary">
                        共 {scheduleList.length} 节
                        {scheduleItems.length > 0 ? ` · 业务日 ${scheduleItems.length} 节` : ''}
                        {scheduleDirty ? ' · 未保存' : ''}
                        {scheduleManualEdited ? ' · 已调课' : ''}
                      </Typography.Text>
                      <Button size="small" icon={<PlusOutlined />} onClick={() => openScheduleEditor()}>
                        添加
                      </Button>
                      {hasKcbiaoFile ? (
                        <Popconfirm
                          title="用开班上传的课表覆盖当前结构化课次？"
                          description={
                            scheduleManualEdited
                              ? '本班已在跟班调过课，将弹出二次确认；开班原 Excel 不会被改写。'
                              : '跟班已改过的课次将被替换；开班原 Excel 文件不会被改写。'
                          }
                          onConfirm={() => runImportKcbiao(true)}
                          disabled={!hasKcbiaoFile}
                        >
                          <Button size="small" icon={<ReloadOutlined />} loading={importKcbiaoMu.isPending}>
                            重导开班课表
                          </Button>
                        </Popconfirm>
                      ) : null}
                      <Button
                        size="small"
                        icon={<ExportOutlined />}
                        disabled={scheduleList.length === 0}
                        loading={exportScheduleMu.isPending}
                        onClick={runExportSchedule}
                      >
                        导出
                      </Button>
                      <Button
                        size="small"
                        type="primary"
                        disabled={!scheduleDirty}
                        loading={saveScheduleMu.isPending}
                        onClick={() => saveScheduleMu.mutate()}
                      >
                        保存课表
                      </Button>
                    </Space>
                  }
                >
                  {scheduleManualEdited ? (
                    <Alert
                      type="warning"
                      showIcon
                      style={{ marginBottom: 8 }}
                      message="当前课表已在跟班手工调整，以结构化课次为准；重导开班 Excel 需二次确认。"
                    />
                  ) : null}
                  {scheduleList.length === 0 ? (
                    <Empty
                      image={Empty.PRESENTED_IMAGE_SIMPLE}
                      description={
                        hasKcbiaoFile
                          ? '尚未导入结构化课次；可从开班上传的课表导入，或直接添加'
                          : '暂无课表：请先在开班准备上传课程表，或在此直接添加课次'
                      }
                    >
                      <Space>
                        {hasKcbiaoFile ? (
                          <Button
                            type="primary"
                            loading={importKcbiaoMu.isPending}
                            onClick={() => runImportKcbiao(false)}
                          >
                            从开班课表导入
                          </Button>
                        ) : null}
                        <Button onClick={() => openScheduleEditor()}>添加课次</Button>
                        {hasKcbiaoFile && (classItem.kcbiao_src || classItem.kcbiao_file) ? (
                          <Button
                            type="link"
                            onClick={() => {
                              const url = classItem.kcbiao_src || classItem.kcbiao_file;
                              if (url) void downloadUrlFile(url, '开班课程表');
                            }}
                          >
                            下载原文件
                          </Button>
                        ) : null}
                      </Space>
                    </Empty>
                  ) : (
                    <Table
                      size="small"
                      rowKey={(r) => r._key}
                      pagination={false}
                      sticky
                      dataSource={scheduleList}
                      tableLayout="fixed"
                      rowClassName={(r) =>
                        hasBizDate && String(r.scheduleDate ?? '') === dateStr
                          ? 'follow-schedule-row-today'
                          : ''
                      }
                      columns={[
                        {
                          title: '日期',
                          dataIndex: 'scheduleDate',
                          width: 70,
                          render: (v: string) => (
                            <Space size={4}>
                              <span>{v || '—'}</span>
                              {hasBizDate && v === dateStr ? <Tag color="processing">今日</Tag> : null}
                            </Space>
                          ),
                        },
                        {
                          title: '星期',
                          dataIndex: 'dayOfWeek',
                          width: 52,
                          render: (v) => dayOfWeekLabel(v) || '—',
                        },
                        {
                          title: '时间',
                          width: 80,
                          render: (_, r) =>
                            `${String(r.startTime || '—')}-${String(r.endTime || '—')}`,
                        },
                        {
                          title: '培训内容',
                          dataIndex: 'courseName',
                          width: 514,
                          ellipsis: true,
                          render: (v?: string) => v || '—',
                        },
                        {
                          title: '学时',
                          dataIndex: 'classHours',
                          width: 30,
                          render: (v?: string) => v || '—',
                        },
                        {
                          title: '授课形式',
                          dataIndex: 'teachForm',
                          width: 60,
                          ellipsis: true,
                          render: (v?: string) => v || '—',
                        },
                        {
                          title: '老师',
                          dataIndex: 'teacherName',
                          width: 60,
                          ellipsis: true,
                          render: (v?: string) => v || '—',
                        },
                        {
                          title: '操作',
                          width: 150,
                          render: (_, r) => (
                            <Space size={0} wrap={false}>
                              <Button
                                type="link"
                                size="small"
                                icon={<EditOutlined />}
                                onClick={() => openScheduleEditor(r)}
                              >
                                直接改课
                              </Button>
                              <Popconfirm title="删除该课次？" onConfirm={() => removeScheduleRow(r._key)}>
                                <Button type="link" size="small" danger icon={<DeleteOutlined />}>
                                  删除
                                </Button>
                              </Popconfirm>
                              <Button
                                type="link"
                                size="small"
                                disabled={!r.scheduleDate}
                                onClick={() => {
                                  if (r.scheduleDate) {
                                    setBizDate(dayjs(String(r.scheduleDate)));
                                    setSessionKey(String(r.id ?? 'day'));
                                    setActiveTab('attendance');
                                  }
                                }}
                              >
                                考勤
                              </Button>
                            </Space>
                          ),
                        },
                      ]}
                    />
                  )}
                </Card>
              </div>
            ),
          },
          {
            key: 'schedule-change',
            label: pendingChangeCount > 0 ? `调课审批 (${pendingChangeCount})` : '调课审批',
            children:
              classIdNum != null && classIdNum > 0 ? (
                <FollowScheduleChangeTab classId={classIdNum} />
              ) : (
                <Empty description="无效班级" />
              ),
          },
          {
            key: 'homework',
            label: '作业',
            children:
              classIdNum != null && classIdNum > 0 ? (
                <FollowHomeworkTab classId={classIdNum} />
              ) : (
                <Empty description="无效班级" />
              ),
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
            key: 'mock',
            label: '模拟考',
            children:
              classIdNum != null && classIdNum > 0 ? (
                <FollowMockExamTab classId={classIdNum} />
              ) : (
                <Empty description="无效班级" />
              ),
          },
          {
            key: 'roles',
            label: '角色',
            children:
              classId != null ? (
                <ClassRolesPanel
                  classId={classId}
                  contextHint="在此指派后，开班准备「班级角色」将同步显示。"
                />
              ) : (
                <Empty description="无效班级" />
              ),
          },
          {
            key: 'evidence',
            label: '留痕',
            children:
              classIdNum != null ? (
                <FollowEvidenceTab classId={classIdNum} />
              ) : (
                <Empty description="无效班级" />
              ),
          },
          {
            key: 'logs',
            label: '日志·督导',
            children:
              classIdNum != null ? (
                <FollowLogsSupervisionTab classId={classIdNum} defaultBizDate={bizDate} />
              ) : (
                <Empty description="无效班级" />
              ),
          },
          {
            key: 'metrics',
            label: '指标',
            children: (
              <Space direction="vertical" style={{ width: '100%' }} size="middle">
                <Row gutter={[12, 12]}>
                  <Col xs={12} md={6}>
                    <Card size="small">
                      <Statistic
                        title="出勤率（当日）"
                        value={attendanceRate}
                        suffix="%"
                        prefix={<CheckCircleOutlined />}
                      />
                    </Card>
                  </Col>
                  <Col xs={12} md={6}>
                    <Card size="small" loading={classStats.loading}>
                      <Statistic
                        title="出勤率（累计）"
                        value={classStats.cumulativeRate}
                        suffix="%"
                        prefix={<LineChartOutlined />}
                      />
                    </Card>
                  </Col>
                  <Col xs={12} md={6}>
                    <Card size="small" loading={qualityMetricsLoading || homeworkMetricsLoading}>
                      <Statistic
                        title="作业完成率"
                        value={qualityMetrics?.homeworkCompletionRate ?? homeworkMetrics?.completionRate ?? '—'}
                        suffix={
                          qualityMetrics?.homeworkCompletionRate != null || homeworkMetrics?.completionRate != null
                            ? '%'
                            : undefined
                        }
                        prefix={<EditOutlined />}
                      />
                    </Card>
                  </Col>
                  <Col xs={12} md={6}>
                    <Card size="small" loading={qualityMetricsLoading}>
                      <Statistic
                        title="满意度"
                        value={qualityMetrics?.satisfactionAvg ?? '—'}
                        suffix={
                          qualityMetrics?.satisfactionCount
                            ? `（${qualityMetrics.satisfactionCount} 份）`
                            : undefined
                        }
                        prefix={<StarOutlined />}
                      />
                    </Card>
                  </Col>
                  <Col xs={12} md={6}>
                    <Card size="small" loading={qualityMetricsLoading || mockMetricsLoading}>
                      <Statistic
                        title="模拟通过率"
                        value={qualityMetrics?.mockPassRate ?? '—'}
                        suffix={qualityMetrics?.mockPassRate != null ? '%' : undefined}
                      />
                    </Card>
                  </Col>
                  <Col xs={12} md={6}>
                    <Card size="small" loading={qualityMetricsLoading}>
                      <Statistic
                        title="考试通过率"
                        value={qualityMetrics?.examPassRate ?? '—'}
                        suffix={qualityMetrics?.examPassRate != null ? '%' : undefined}
                      />
                    </Card>
                  </Col>
                  <Col xs={12} md={6}>
                    <Card size="small" loading={qualityMetricsLoading}>
                      <Statistic
                        title="影像留痕"
                        value={
                          qualityMetrics?.evidenceRequiredSessions
                            ? `${qualityMetrics.evidenceOkSessions ?? 0}/${qualityMetrics.evidenceRequiredSessions}`
                            : '—'
                        }
                        suffix={qualityMetrics?.evidenceReady ? '齐套' : undefined}
                      />
                    </Card>
                  </Col>
                  <Col xs={12} md={6}>
                    <Card size="small" loading={qualityMetricsLoading}>
                      <Statistic
                        title="补贴材料齐套"
                        value={
                          qualityMetrics?.subsidyId == null
                            ? '未建单'
                            : qualityMetrics.subsidyTotalSlots
                              ? `${qualityMetrics.subsidyReadySlots ?? 0}/${qualityMetrics.subsidyTotalSlots}`
                              : qualityMetrics.subsidyReady
                                ? '已齐套'
                                : '—'
                        }
                        valueStyle={{
                          color:
                            qualityMetrics?.subsidyReady === true
                              ? '#3f8600'
                              : qualityMetrics?.subsidyReady === false
                                ? '#cf1322'
                                : undefined,
                        }}
                      />
                    </Card>
                  </Col>
                </Row>

                <Card size="small" title="质量预警" loading={qualityMetricsLoading}>
                  {(qualityMetrics?.alerts?.length ?? 0) === 0 ? (
                    <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无预警" />
                  ) : (
                    <Space direction="vertical" style={{ width: '100%' }}>
                      {(qualityMetrics?.alerts ?? []).map((a, idx) => (
                        <Alert
                          key={`${a.code}-${idx}`}
                          type={a.level === 'critical' ? 'error' : 'warning'}
                          showIcon
                          message={a.message || a.code}
                        />
                      ))}
                    </Space>
                  )}
                </Card>

                <Alert
                  type="info"
                  showIcon
                  message="口径说明"
                  description={
                    <>
                      当日出勤=实到÷名单人数；累计出勤=有效出勤记录÷全部考勤记录（覆盖 {classStats.recordDays}{' '}
                      个有考勤日）。作业完成率=已交有效份数÷应发份数。模拟通过率=已评通过份数÷已评份数。考试通过率=得分≥及格线人数÷有成绩人数。满意度=答卷均分（1～5）。补贴齐套=配方必填槽位均已有材料（或已生成材料包）。
                      预警阈值：累计出勤/作业完成率 &lt;80%、满意度 &lt;3.5、影像未齐套、严重整改未关、补贴缺项。
                    </>
                  }
                />
              </Space>
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

      <Modal
        title={editingScheduleKey ? '直接改课' : '添加课次'}
        open={scheduleModalOpen}
        onCancel={() => setScheduleModalOpen(false)}
        onOk={() => void submitScheduleEditor()}
        destroyOnHidden
        width={520}
      >
        <Form form={scheduleForm} layout="vertical">
          <Form.Item name="scheduleDate" label="日期" rules={[{ required: true, message: '请选择日期' }]}>
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            name="timeRange"
            label="时间"
            rules={[{ required: true, message: '请选择起止时间' }]}
          >
            <TimePicker.RangePicker format="HH:mm" style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="courseName" label="培训内容" rules={[{ required: true, message: '请输入培训内容' }]}>
            <Input placeholder="培训内容 / 课程名称" />
          </Form.Item>
          <Form.Item name="classHours" label="学时">
            <Input placeholder="如 2" />
          </Form.Item>
          <Form.Item name="teachForm" label="授课形式">
            <Select
              allowClear
              placeholder="选择或留空"
              options={[
                { value: '理论', label: '理论' },
                { value: '实操', label: '实操' },
              ]}
            />
          </Form.Item>
          <Form.Item name="teacherName" label="老师">
            <Input placeholder="老师姓名" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

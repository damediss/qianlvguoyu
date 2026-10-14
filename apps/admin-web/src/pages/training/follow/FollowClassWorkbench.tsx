import { useDeferredValue, useEffect, useMemo, useRef, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Col,
  DatePicker,
  Empty,
  Form,
  Input,
  InputNumber,
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
import { AttendanceExportButton } from './AttendanceExportButton';
import type { ClassStudent, TrainInfoClassListItem } from '../../../types/training';
import { StudentAttendanceDetailDrawer } from './StudentAttendanceDetailDrawer';
import { FollowPracticeTab } from './FollowPracticeTab';
import { FollowReviewSyncTab } from './FollowReviewSyncTab';
import { FollowScheduleChangeTab } from './FollowScheduleChangeTab';
import { FollowHomeworkTab } from './FollowHomeworkTab';
import { FollowMockExamTab } from './FollowMockExamTab';
import { FollowEvidenceTab } from './FollowEvidenceTab';
import { FollowLogsSupervisionTab } from './FollowLogsSupervisionTab';
import { ClassRolesPanel } from '../ClassRolesPanel';
import { useNavStore } from '../../../stores/navStore';

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

function formatClassHours(value?: number | string | null): string {
  if (value == null || value === '') return '—';
  const n = typeof value === 'number' ? value : Number(value);
  if (!Number.isFinite(n)) return '—';
  return n.toFixed(1);
}

/** 上课时段：按开始时刻划分上午/下午（12 点前为上午） */
type DayPeriod = '上午' | '下午';

/**
 * 从「HH:mm」或「yyyy-MM-dd HH:mm:ss」等串中取小时。
 * 注意：不可用 ^(\\d{1,2}) 直接匹配，否则会把日期里的「2026」误读成 20 点 → 全变成下午。
 */
function parseHour(time?: string | null): number | null {
  const raw = String(time ?? '').trim();
  if (!raw) return null;
  // 优先匹配时分：空格/T 后，或整串就是 HH:mm[:ss]
  const m =
    raw.match(/(?:[T\s]|^\s*)(\d{1,2}):(\d{2})(?::\d{2})?/)
    ?? raw.match(/^(\d{1,2}):(\d{2})(?::\d{2})?$/);
  if (!m) return null;
  const h = Number(m[1]);
  return Number.isFinite(h) && h >= 0 && h <= 23 ? h : null;
}

/** 无法判断时段时返回 null（不归入「全天」） */
function periodFromTime(time?: string | null): DayPeriod | null {
  const h = parseHour(time);
  if (h == null) return null;
  return h < 12 ? '上午' : '下午';
}

/** 从导入备注「导入课次：08:30-11:30」取上课开始时间 */
function startTimeFromAttendanceRemark(remark?: string | null): string | null {
  const r = String(remark ?? '');
  const m = r.match(/导入课次：\s*(\d{1,2}:\d{2})/);
  return m ? m[1] : null;
}

/** 从导入备注解析起止时间 */
function sessionTimesFromAttendanceRemark(
  remark?: string | null,
): { start: string | null; end: string | null } {
  const r = String(remark ?? '');
  const m = r.match(/导入课次：\s*(\d{1,2}:\d{2})\s*[-–—~～]\s*(\d{1,2}:\d{2})/);
  if (m) return { start: m[1], end: m[2] };
  return { start: startTimeFromAttendanceRemark(remark), end: null };
}

/** 统一成 YYYY-MM-DD，兼容带时间 / 斜杠日期 */
function normalizeBizDate(raw?: string | null): string {
  const s = String(raw ?? '').trim();
  if (!s) return '';
  const iso = s.match(/^(\d{4})-(\d{1,2})-(\d{1,2})/);
  if (iso) {
    return `${iso[1]}-${iso[2].padStart(2, '0')}-${iso[3].padStart(2, '0')}`;
  }
  const slash = s.match(/^(\d{4})\/(\d{1,2})\/(\d{1,2})/);
  if (slash) {
    return `${slash[1]}-${slash[2].padStart(2, '0')}-${slash[3].padStart(2, '0')}`;
  }
  return s.slice(0, 10);
}

/** 取 HH:mm，兼容完整日期时间串 */
function extractHm(time?: string | null): string | null {
  const raw = String(time ?? '').trim();
  if (!raw) return null;
  const m = raw.match(/(?:[T\s]|^)(\d{1,2}):(\d{2})(?::\d{2})?/);
  if (!m) return null;
  return `${m[1].padStart(2, '0')}:${m[2]}`;
}

function formatSessionLabel(
  startTime?: string | null,
  endTime?: string | null,
  fallback?: string,
): string {
  const start = extractHm(startTime);
  const end = extractHm(endTime);
  if (!start && !end) return fallback || '课次';
  return `${start || '—'}-${end || '—'}`;
}

/** 出勤优先：出勤 > 迟到 > 早退 > 缺勤 */
function attendanceStatusRank(status?: number | null): number {
  if (status === 1) return 4;
  if (status === 2) return 3;
  if (status === 3) return 2;
  if (status === 0) return 1;
  return 0;
}

/** 同日多课次合并为一条展示（任一有效出勤即出勤；签到时间拼接） */
function mergeDayAttendanceRows(
  list: trainApi.TrainAttendanceItem[],
): trainApi.TrainAttendanceItem {
  const sessions = list.filter((r) => r.scheduleId != null);
  const dayLevel = list.find((r) => r.scheduleId == null || r.scheduleId === undefined);
  const pool = sessions.length > 0 ? sessions : dayLevel ? [dayLevel] : list;

  let best = pool[0];
  for (const row of pool) {
    if (attendanceStatusRank(row.status) > attendanceStatusRank(best.status)) {
      best = row;
    }
  }

  const checkIns = pool
    .map((r) => String(r.checkInTime ?? '').trim())
    .filter(Boolean);
  const uniqueCheckIns = [...new Set(checkIns)];
  const remarks = pool
    .map((r) => String(r.remark ?? '').trim())
    .filter(Boolean);
  const uniqueRemarks = [...new Set(remarks)];

  return {
    ...best,
    checkInTime:
      uniqueCheckIns.length > 1 ? uniqueCheckIns.join(' / ') : uniqueCheckIns[0] ?? best.checkInTime,
    remark:
      uniqueRemarks.length > 1 ? uniqueRemarks.join('；') : uniqueRemarks[0] ?? best.remark,
  };
}

type SessionPeriodLabel = { period: DayPeriod; timeLabel: string; sortKey: string };

/** 从考勤行解析「上午 08:30-11:30」类标注 */
function resolveSessionPeriodLabel(
  row: trainApi.TrainAttendanceItem,
  schedules: Array<{ id?: number | string; startTime?: string; endTime?: string }>,
): SessionPeriodLabel | null {
  const sch =
    row.scheduleId != null
      ? schedules.find((s) => String(s.id) === String(row.scheduleId))
      : undefined;
  const fromRemark = sessionTimesFromAttendanceRemark(row.remark);
  const start =
    (sch?.startTime != null ? String(sch.startTime) : null)
    || fromRemark.start
    || extractHm(row.checkInTime);
  const end =
    (sch?.endTime != null ? String(sch.endTime) : null)
    || fromRemark.end
    || null;
  const period =
    periodFromTime(start)
    || periodFromTime(fromRemark.start)
    || periodFromTime(row.checkInTime);
  if (!period) return null;
  const timeLabel = formatSessionLabel(start, end, '');
  return {
    period,
    timeLabel: timeLabel && timeLabel !== '课次' ? timeLabel : '',
    sortKey: scheduleStartSortKey(start),
  };
}

const PERIOD_SORT: Record<DayPeriod, number> = { 上午: 0, 下午: 1 };

function scheduleStartSortKey(startTime?: string | null): string {
  const h = parseHour(startTime);
  const m = String(startTime ?? '').match(/:(\d{2})/);
  const mm = m ? m[1] : '00';
  if (h == null) return '99:99';
  return `${String(h).padStart(2, '0')}:${mm}`;
}

/** 按日期+上午/下午挑选课次；上午取最早一节，下午取最晚一节 */
function pickScheduleForPeriod(
  schedules: Array<{ id?: number | string; scheduleDate?: string; startTime?: string }>,
  date: string,
  period: DayPeriod,
  preferScheduleIds?: Set<string>,
): { id?: number | string; startTime?: string } | null {
  const candidates = schedules.filter((s) => {
    if (normalizeBizDate(s.scheduleDate != null ? String(s.scheduleDate) : null) !== date) return false;
    return periodFromTime(s.startTime != null ? String(s.startTime) : null) === period;
  });
  if (candidates.length === 0) return null;
  const preferred =
    preferScheduleIds && preferScheduleIds.size > 0
      ? candidates.filter((s) => s.id != null && preferScheduleIds.has(String(s.id)))
      : [];
  const pool = preferred.length > 0 ? preferred : candidates;
  pool.sort((a, b) =>
    scheduleStartSortKey(a.startTime != null ? String(a.startTime) : null).localeCompare(
      scheduleStartSortKey(b.startTime != null ? String(b.startTime) : null),
    ),
  );
  return period === '下午' ? pool[pool.length - 1] : pool[0];
}

/** 师资库下拉：可搜索、可选手动输入姓名（antd Select 搜索不自动提交，用 onSearch/onBlur 提交）。 */
function TeacherBankSelect({ value, onChange }: { value?: string; onChange?: (v?: string) => void }) {
  const [searchText, setSearchText] = useState('');
  const deferredKeyword = useDeferredValue(searchText.trim());

  const { data, isFetching } = useQuery({
    queryKey: ['teacher-banks-pick', deferredKeyword],
    queryFn: () =>
      trainApi.pageTeacherBanks({
        page: 1,
        pageSize: 50,
        keyword: deferredKeyword || undefined,
      }),
  });

  const bankOptions = useMemo(() => {
    const items = data?.items ?? [];
    const seen = new Set<string>();
    const opts: { value: string; label: string }[] = [];
    for (const t of items) {
      const name = String(t.name ?? '').trim();
      if (!name || seen.has(name)) continue;
      seen.add(name);
      opts.push({
        value: name,
        label: t.phone ? `${name} · ${t.phone}` : name,
      });
    }
    return opts;
  }, [data?.items]);

  const options = useMemo(() => {
    const all = [...bankOptions];
    const current = String(value ?? '').trim();
    if (current && !all.some((o) => o.value === current)) {
      all.unshift({ value: current, label: current });
    }
    const typed = searchText.trim();
    if (typed && !all.some((o) => o.value === typed)) {
      all.unshift({ value: typed, label: `${typed}（手动输入）` });
    }
    return all;
  }, [bankOptions, value, searchText]);

  const commit = (raw?: string) => {
    const next = raw?.trim() || undefined;
    if (next !== value) onChange?.(next);
  };

  return (
    <Select
      showSearch
      allowClear
      placeholder="从师资库选择或手动输入"
      loading={isFetching}
      value={value || undefined}
      searchValue={searchText}
      options={options}
      filterOption={false}
      notFoundContent={isFetching ? '加载中…' : '无匹配，回车或失焦可手动录入'}
      onSearch={setSearchText}
      onChange={(v) => {
        setSearchText('');
        commit(v as string | undefined);
      }}
      onBlur={() => {
        if (searchText.trim() && searchText.trim() !== value) commit(searchText);
      }}
      onInputKeyDown={(e) => {
        if (e.key === 'Enter' && searchText.trim() && searchText.trim() !== value) {
          e.preventDefault();
          commit(searchText);
          setSearchText('');
        }
      }}
      style={{ width: '100%' }}
    />
  );
}

type Props = {
  classItem: TrainInfoClassListItem;
  onBack: () => void;
};

/** 分页拉齐考勤，避免单页 5000 截断导致累计统计不准 */
async function fetchAllAttendances(params: {
  classId: number;
  attendanceDate?: string;
  scheduleId?: number;
  unscheduledOnly?: boolean;
}): Promise<{ items: trainApi.TrainAttendanceItem[]; total: number }> {
  const pageSize = 500;
  const maxPages = 200;
  const items: trainApi.TrainAttendanceItem[] = [];
  let page = 1;
  let total = Number.POSITIVE_INFINITY;
  while (items.length < total && page <= maxPages) {
    const res = await trainApi.pageAttendances({
      ...params,
      page,
      pageSize,
    });
    const batch = res.items ?? [];
    items.push(...batch);
    total = typeof res.total === 'number' ? res.total : items.length;
    if (batch.length === 0) break;
    page += 1;
  }
  if (items.length < total) {
    message.warning(`考勤记录较多，仅加载 ${items.length}/${total} 条，累计统计可能偏少`);
  }
  return { items, total };
}

export function FollowClassWorkbench({ classItem, onBack }: Props) {
  const queryClient = useQueryClient();
  const navigateTo = useNavStore((s) => s.setSelectedKey);
  const classId = classItem.id;
  const trainInfoId = classItem.train_info_id;
  const classIdNum = toNumId(classId);

  const openClassSubsidy = async () => {
    if (classIdNum == null) {
      message.error('无效班级');
      return;
    }
    try {
      const existing = await trainApi.getClassSubsidy(classIdNum);
      if (existing) {
        trainApi.stashSubsidyDeclareFocus({ classId: classIdNum, subsidyId: existing.id });
        navigateTo('train_subsidy_declare');
        return;
      }
      const created = await trainApi.ensureClassSubsidy(classIdNum);
      message.success(created.phase === 'draft' ? '已为本班创建补贴草稿' : '已打开本班补贴单');
      trainApi.stashSubsidyDeclareFocus({ classId: classIdNum, subsidyId: created.id });
      navigateTo('train_subsidy_declare');
      void queryClient.invalidateQueries({ queryKey: ['follow-quality-metrics', classIdNum] });
    } catch (e) {
      const msg = (e as Error).message || '打开补贴失败';
      message.error(msg);
      trainApi.stashSubsidyDeclareFocus({ classId: classIdNum, openCreate: true });
      navigateTo('train_subsidy_declare');
    }
  };

  const [activeTab, setActiveTab] = useState('overview');
  const [bizDate, setBizDate] = useState<Dayjs | null>(dayjs());
  const [sessionKey, setSessionKey] = useState<string>('day');
  /** 概览「查看」：先改日期，待课表选项就绪后再切到对应上下午课次 */
  const [pendingPeriodNav, setPendingPeriodNav] = useState<{
    date: string;
    period: DayPeriod;
  } | null>(null);
  const [studentPage, setStudentPage] = useState(1);
  const [detailStudent, setDetailStudent] = useState<ClassStudent | null>(null);
  const [importResult, setImportResult] = useState<trainApi.TrainAttendanceImportResult | null>(null);
  const [scheduleDraft, setScheduleDraft] = useState<ScheduleDraftItem[]>([]);
  const [scheduleDirty, setScheduleDirty] = useState(false);
  const [scheduleModalOpen, setScheduleModalOpen] = useState(false);
  const [editingScheduleKey, setEditingScheduleKey] = useState<string | null>(null);
  const [scheduleForm] = Form.useForm();
  /** 课程表 Tab：课表列表 | 教师上课时长 */
  const [schedulePanel, setSchedulePanel] = useState<'list' | 'teacherHours'>('list');
  /** 本班课酬编辑草稿：key = teacherName|teachForm */
  const [teacherPayDraft, setTeacherPayDraft] = useState<
    Record<string, { ratePerDay?: number | null }>
  >({});
  const teacherPayDraftRef = useRef(teacherPayDraft);
  teacherPayDraftRef.current = teacherPayDraft;
  const teacherPaySaveTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const teacherPaySavingRef = useRef(false);

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
    return scheduleList.filter(
      (s) => normalizeBizDate(s.scheduleDate != null ? String(s.scheduleDate) : null) === dateStr,
    );
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
    mutationFn: (items?: ScheduleDraftItem[]) => {
      if (classIdNum == null) return Promise.reject(new Error('无效班级'));
      const source = items ?? scheduleDraft;
      const payload = source.map(({ _key, ...rest }) => {
        void _key;
        return rest;
      });
      return trainApi.saveClassSchedule(classIdNum, payload);
    },
    onSuccess: () => {
      message.success('课表已保存');
      setScheduleDirty(false);
      queryClient.invalidateQueries({ queryKey: ['follow-schedule', classIdNum] });
      queryClient.invalidateQueries({ queryKey: ['follow-class-hours', classIdNum] });
      queryClient.invalidateQueries({ queryKey: ['follow-teacher-days', classIdNum] });
      queryClient.invalidateQueries({ queryKey: ['follow-quality-metrics', classIdNum] });
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
      queryClient.invalidateQueries({ queryKey: ['follow-class-hours', classIdNum] });
      queryClient.invalidateQueries({ queryKey: ['follow-teacher-days', classIdNum] });
      queryClient.invalidateQueries({ queryKey: ['follow-quality-metrics', classIdNum] });
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
    const nextDraft = editingScheduleKey
      ? scheduleDraft.map((x) => (x._key === editingScheduleKey ? next : x))
      : [...scheduleDraft, next];
    setScheduleDraft(nextDraft);
    setScheduleDirty(true);
    setScheduleModalOpen(false);
    await saveScheduleMu.mutateAsync(nextDraft);
  };

  const removeScheduleRow = async (key: string) => {
    const nextDraft = scheduleDraft.filter((x) => x._key !== key);
    setScheduleDraft(nextDraft);
    setScheduleDirty(true);
    await saveScheduleMu.mutateAsync(nextDraft);
  };

  const { data: studentsData, isLoading: studentsLoading } = useQuery({
    queryKey: ['follow-students', classId, studentPage],
    queryFn: () => trainApi.listClassStudents(classId, studentPage, 50),
    enabled: !!classId,
  });

  const students = studentsData?.list ?? [];
  const studentTotal = studentsData?.total ?? 0;

  /** 概览统计用全量花名册（分页拉齐，避免 >500 人时当日实到不准） */
  const { data: allStudentsData } = useQuery({
    queryKey: ['follow-students-all', classId],
    queryFn: async () => {
      const pageSize = 200;
      const list: NonNullable<Awaited<ReturnType<typeof trainApi.listClassStudents>>['list']> = [];
      let page = 1;
      let total = Number.POSITIVE_INFINITY;
      const maxPages = 100;
      while (list.length < total && page <= maxPages) {
        const res = await trainApi.listClassStudents(classId, page, pageSize);
        const batch = res.list ?? [];
        list.push(...batch);
        total = typeof res.total === 'number' ? res.total : list.length;
        if (batch.length === 0) break;
        page += 1;
      }
      if (list.length < total) {
        message.warning(`本班花名册较大，概览仅加载 ${list.length}/${total} 人，当日实到可能偏少`);
      }
      return { list, total };
    },
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
      fetchAllAttendances({
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
    queryFn: () => fetchAllAttendances({ classId: classIdNum! }),
    enabled: classIdNum != null && classIdNum > 0,
    retry: false,
  });

  /** 近日出勤率「查看」：业务日落地后再绑定上午/下午课次，避免仍停在按日或上午 */
  useEffect(() => {
    if (!pendingPeriodNav) return;
    if (dateStr !== pendingPeriodNav.date) return;

    const preferIds = new Set<string>();
    for (const row of allAttendancePage?.items ?? []) {
      if (normalizeBizDate(row.attendanceDate) !== pendingPeriodNav.date) continue;
      if (row.scheduleId == null) continue;
      const sch = scheduleList.find((s) => String(s.id) === String(row.scheduleId));
      const period =
        periodFromTime(sch?.startTime != null ? String(sch.startTime) : null)
        || periodFromTime(startTimeFromAttendanceRemark(row.remark))
        || periodFromTime(row.checkInTime);
      if (period === pendingPeriodNav.period) {
        preferIds.add(String(row.scheduleId));
      }
    }

    const pick = pickScheduleForPeriod(
      scheduleList,
      pendingPeriodNav.date,
      pendingPeriodNav.period,
      preferIds,
    );
    if (pick?.id != null) {
      setSessionKey(String(pick.id));
      setPendingPeriodNav(null);
      return;
    }
    if (preferIds.size > 0) {
      const ids = [...preferIds].sort((a, b) => {
        const sa = scheduleList.find((s) => String(s.id) === a);
        const sb = scheduleList.find((s) => String(s.id) === b);
        return scheduleStartSortKey(sa?.startTime != null ? String(sa.startTime) : null).localeCompare(
          scheduleStartSortKey(sb?.startTime != null ? String(sb.startTime) : null),
        );
      });
      const id = pendingPeriodNav.period === '下午' ? ids[ids.length - 1] : ids[0];
      setSessionKey(id);
      setPendingPeriodNav(null);
      return;
    }
    if (scheduleLoading) return;
    setSessionKey('day');
    setPendingPeriodNav(null);
    message.info(`当日暂无「${pendingPeriodNav.period}」课次，已切换为按日汇总`);
  }, [
    pendingPeriodNav,
    dateStr,
    scheduleList,
    scheduleLoading,
    allAttendancePage?.items,
  ]);

  /**
   * 课次下拉：
   * 1) 当日课表课次
   * 2) 当日考勤已绑定的 scheduleId（课表被重导/保存删掉后仍可筛）
   * 3) 当前选中项兜底 label
   */
  const sessionSelectOptions = useMemo(() => {
    const byId = new Map<string, { value: string; label: string; sortKey: string }>();

    const putSession = (
      id: string,
      start?: string | null,
      end?: string | null,
      fallback?: string,
    ) => {
      if (!id || byId.has(id)) return;
      const label = formatSessionLabel(start, end, fallback || `课次#${id}`);
      byId.set(id, {
        value: id,
        label,
        sortKey: scheduleStartSortKey(start),
      });
    };

    for (const s of scheduleItems) {
      if (s.id == null) continue;
      putSession(
        String(s.id),
        s.startTime != null ? String(s.startTime) : null,
        s.endTime != null ? String(s.endTime) : null,
      );
    }

    // 当日考勤里的课次：即使课表里已无该日/该 ID，也要能筛（导入补建课次后被物理删是常见情况）
    for (const row of allAttendancePage?.items ?? []) {
      if (normalizeBizDate(row.attendanceDate) !== dateStr) continue;
      if (row.scheduleId == null) continue;
      const id = String(row.scheduleId);
      if (byId.has(id)) continue;
      const sch = scheduleList.find((x) => String(x.id) === id);
      if (sch) {
        putSession(
          id,
          sch.startTime != null ? String(sch.startTime) : null,
          sch.endTime != null ? String(sch.endTime) : null,
        );
      } else {
        const times = sessionTimesFromAttendanceRemark(row.remark);
        putSession(id, times.start, times.end, `课次#${id}（考勤）`);
      }
    }

    if (sessionKey !== 'day' && !byId.has(sessionKey)) {
      const s = scheduleList.find((x) => String(x.id) === sessionKey);
      if (s) {
        putSession(
          sessionKey,
          s.startTime != null ? String(s.startTime) : null,
          s.endTime != null ? String(s.endTime) : null,
        );
      } else {
        const att = (allAttendancePage?.items ?? []).find(
          (r) => r.scheduleId != null && String(r.scheduleId) === sessionKey,
        );
        const times = sessionTimesFromAttendanceRemark(att?.remark);
        putSession(sessionKey, times.start, times.end, `课次#${sessionKey}`);
      }
    }

    const sorted = [...byId.values()].sort((a, b) => {
      const c = a.sortKey.localeCompare(b.sortKey);
      return c !== 0 ? c : a.label.localeCompare(b.label, 'zh-CN');
    });
    const fromSchedule = scheduleItems.length > 0;
    return [
      {
        value: 'day',
        label: fromSchedule || sorted.length > 0 ? '按日（无指定课次）' : '按日跟班（当日无排课）',
      },
      ...sorted.map(({ value, label }) => ({ value, label })),
    ];
  }, [scheduleItems, scheduleList, sessionKey, dateStr, allAttendancePage?.items]);

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

  const { data: classHoursData, isLoading: classHoursLoading } = useQuery({
    queryKey: ['follow-class-hours', classIdNum],
    queryFn: () => trainApi.getFollowClassHours(classIdNum!),
    enabled: classIdNum != null && classIdNum > 0,
    retry: false,
  });

  const { data: teacherDaysData, isLoading: teacherDaysLoading } = useQuery({
    queryKey: ['follow-teacher-days', classIdNum],
    queryFn: () => trainApi.getFollowTeacherDays(classIdNum!),
    enabled: classIdNum != null && classIdNum > 0 && schedulePanel === 'teacherHours',
    retry: false,
  });

  useEffect(() => {
    if (teacherPaySavingRef.current) return;
    const next: Record<string, { ratePerDay?: number | null }> = {};
    for (const row of teacherDaysData?.teachers ?? []) {
      const key = `${row.teacherName}|${row.teachForm ?? '未填'}`;
      next[key] = { ratePerDay: row.ratePerDay ?? null };
    }
    setTeacherPayDraft(next);
  }, [teacherDaysData]);

  const teacherPayDisplaySum = useMemo(() => {
    let sum = 0;
    let any = false;
    for (const row of teacherDaysData?.teachers ?? []) {
      const key = `${row.teacherName}|${row.teachForm ?? '未填'}`;
      const draft = teacherPayDraft[key];
      const rate = draft?.ratePerDay ?? row.ratePerDay;
      const days = row.taughtDays;
      if (rate != null && days != null) {
        sum += Math.round(Number(rate) * Number(days));
        any = true;
      }
    }
    return any ? sum : null;
  }, [teacherDaysData, teacherPayDraft]);

  const saveTeacherPayMu = useMutation({
    mutationFn: (items: Array<{
      teacherName: string;
      teachForm?: string;
      ratePerDay?: number | null;
    }>) => trainApi.saveFollowTeacherPay(classIdNum!, items),
    onSuccess: (data) => {
      teacherPaySavingRef.current = false;
      queryClient.setQueryData(['follow-teacher-days', classIdNum], data);
      message.success('课酬已保存', 1);
    },
    onError: (e: Error) => {
      teacherPaySavingRef.current = false;
      message.error(e.message || '保存课酬失败');
    },
  });

  const buildTeacherPayItems = (
    draft: Record<string, { ratePerDay?: number | null }>,
  ) =>
    (teacherDaysData?.teachers ?? []).map((row) => {
      const key = `${row.teacherName}|${row.teachForm ?? '未填'}`;
      return {
        teacherName: row.teacherName,
        teachForm: row.teachForm ?? '未填',
        ratePerDay: (draft[key] ?? {}).ratePerDay ?? null,
      };
    });

  const flushSaveTeacherPay = (draft?: Record<string, { ratePerDay?: number | null }>) => {
    if (teacherPaySaveTimerRef.current) {
      clearTimeout(teacherPaySaveTimerRef.current);
      teacherPaySaveTimerRef.current = null;
    }
    if (classIdNum == null || classIdNum <= 0) return;
    const items = buildTeacherPayItems(draft ?? teacherPayDraftRef.current);
    if (items.length === 0) return;
    teacherPaySavingRef.current = true;
    saveTeacherPayMu.mutate(items);
  };

  const scheduleSaveTeacherPay = (draft: Record<string, { ratePerDay?: number | null }>) => {
    teacherPayDraftRef.current = draft;
    if (teacherPaySaveTimerRef.current) {
      clearTimeout(teacherPaySaveTimerRef.current);
    }
    teacherPaySaveTimerRef.current = setTimeout(() => {
      teacherPaySaveTimerRef.current = null;
      flushSaveTeacherPay(draft);
    }, 600);
  };

  useEffect(
    () => () => {
      if (teacherPaySaveTimerRef.current) {
        clearTimeout(teacherPaySaveTimerRef.current);
      }
    },
    [],
  );

  const exportTeacherDaysMu = useMutation({
    mutationFn: () => trainApi.exportFollowTeacherDays(classIdNum!),
    onSuccess: () => message.success('已开始下载'),
    onError: (e: Error) => message.error(e.message || '导出失败'),
  });

  const hoursByPeopleId = useMemo(() => {
    const map = new Map<string, trainApi.FollowStudentClassHours>();
    for (const row of classHoursData?.students ?? []) {
      if (row.peopleId != null) map.set(String(row.peopleId), row);
    }
    return map;
  }, [classHoursData?.students]);

  /** 当日考勤行：合并「按日查询」与全量缓存，避免只拿到一批课次 */
  const dayAttendanceRows = useMemo(() => {
    if (!hasBizDate) return [];
    const byId = new Map<string | number, trainApi.TrainAttendanceItem>();
    for (const row of allAttendancePage?.items ?? []) {
      if (normalizeBizDate(row.attendanceDate) !== dateStr) continue;
      if (row.id != null) byId.set(row.id, row);
    }
    for (const row of attendancePage?.items ?? []) {
      if (normalizeBizDate(row.attendanceDate) !== dateStr) continue;
      if (row.id != null) byId.set(row.id, row);
    }
    return [...byId.values()];
  }, [hasBizDate, dateStr, allAttendancePage?.items, attendancePage?.items]);

  const attendanceMap = useMemo(() => {
    const map = new Map<string, trainApi.TrainAttendanceItem>();
    if (isSessionAttendance) {
    for (const row of attendancePage?.items ?? []) {
      if (row.userId != null) map.set(String(row.userId), row);
    }
    return map;
    }
    // 按日：合并当日所有课次；有课次记录时不再被「按日空 schedule」行盖住
    const byUser = new Map<string, trainApi.TrainAttendanceItem[]>();
    for (const row of dayAttendanceRows) {
      if (row.userId == null) continue;
      const key = String(row.userId);
      const list = byUser.get(key) ?? [];
      list.push(row);
      byUser.set(key, list);
    }
    for (const [uid, list] of byUser) {
      map.set(uid, mergeDayAttendanceRows(list));
    }
    return map;
  }, [attendancePage?.items, dayAttendanceRows, isSessionAttendance]);

  /** 学员 → 当日/本课次上下午上课时间标注 */
  const attendancePeriodLabelsByUser = useMemo(() => {
    const map = new Map<string, string[]>();
    const sourceRows = isSessionAttendance
      ? (attendancePage?.items ?? [])
      : dayAttendanceRows;
    const byUser = new Map<string, trainApi.TrainAttendanceItem[]>();
    for (const row of sourceRows) {
      if (row.userId == null) continue;
      const key = String(row.userId);
      const list = byUser.get(key) ?? [];
      list.push(row);
      byUser.set(key, list);
    }
    for (const [uid, list] of byUser) {
      const seen = new Map<string, SessionPeriodLabel>();
      for (const row of list) {
        const info = resolveSessionPeriodLabel(row, scheduleList);
        if (!info) continue;
        const key = `${info.period}|${info.timeLabel}`;
        if (!seen.has(key)) seen.set(key, info);
      }
      const labels = [...seen.values()]
        .sort((a, b) => {
          const pc = PERIOD_SORT[a.period] - PERIOD_SORT[b.period];
          return pc !== 0 ? pc : a.sortKey.localeCompare(b.sortKey);
        })
        .map((x) => (x.timeLabel ? `${x.period} ${x.timeLabel}` : x.period));
      if (labels.length) map.set(uid, labels);
    }
    return map;
  }, [
    isSessionAttendance,
    attendancePage?.items,
    dayAttendanceRows,
    scheduleList,
  ]);

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
    const expected = rosterTotal > 0 ? rosterTotal : allStudents.length;
    const nameById = new Map(allStudents.map((s) => [String(s.id), s.realname]));

    /** scheduleId → 上课日 + 上下午；date → 当日课表出现过的时段（来自导入/排课） */
    const schedulePeriod = new Map<string, { date: string; period: DayPeriod }>();
    const periodsByDate = new Map<string, Set<DayPeriod>>();
    for (const s of scheduleList) {
      const date = String(s.scheduleDate ?? '').slice(0, 10);
      const period = periodFromTime(s.startTime != null ? String(s.startTime) : null);
      if (!date || !period) continue;
      if (s.id != null) {
        schedulePeriod.set(String(s.id), { date, period });
      }
      const set = periodsByDate.get(date) ?? new Set<DayPeriod>();
      set.add(period);
      periodsByDate.set(date, set);
    }

    type SlotAgg = {
      date: string;
      period: DayPeriod;
      presentUsers: Set<string>;
      absentUsers: Set<string>;
    };
    const bySlot = new Map<string, SlotAgg>();

    const byUser = new Map<
      string,
      { name: string; present: number; absent: number; late: number; early: number; total: number }
    >();

    /** 先扫一遍：哪些「日」已有课次考勤（有 scheduleId） */
    const datesWithSessionAttendance = new Set<string>();
    for (const row of rows) {
      const date = String(row.attendanceDate ?? '').slice(0, 10);
      if (date && row.scheduleId != null) datesWithSessionAttendance.add(date);
    }

    const resolvePeriod = (row: trainApi.TrainAttendanceItem): DayPeriod | null => {
      const date = String(row.attendanceDate ?? '').slice(0, 10);
      if (!date) return null;

      // 1) 绑定课次：课表开始时间
      if (row.scheduleId != null) {
        const meta = schedulePeriod.get(String(row.scheduleId));
        if (meta?.period) return meta.period;
      }

      // 2) 导入备注里的课次开始时间「导入课次：08:30-11:30」
      const fromRemark = periodFromTime(startTimeFromAttendanceRemark(row.remark));
      if (fromRemark) return fromRemark;

      // 3) 签到时间（完整日期时间须解析其中的时分，不能误读年份）
      const fromCheckIn = periodFromTime(row.checkInTime);
      if (fromCheckIn) return fromCheckIn;

      // 4) 按日记录：若同日已有课次考勤，不再单独占一行
      if (row.scheduleId == null && datesWithSessionAttendance.has(date)) {
        return null;
      }

      // 5) 当日课表仅有一个时段时，归入该时段
      const dayPeriods = periodsByDate.get(date);
      if (dayPeriods && dayPeriods.size === 1) {
        return [...dayPeriods][0];
      }

      return null;
    };

    for (const row of rows) {
      const date = String(row.attendanceDate ?? '').slice(0, 10);
      if (!date) continue;
      const period = resolvePeriod(row);
      if (!period) continue;

      const slotKey = `${date}|${period}`;
      const slot =
        bySlot.get(slotKey)
        ?? { date, period, presentUsers: new Set<string>(), absentUsers: new Set<string>() };
      const uid = row.userId != null ? String(row.userId) : '';
      const status = row.status;
      if (uid) {
        if (status != null && status > 0) {
          slot.presentUsers.add(uid);
          slot.absentUsers.delete(uid);
        } else if (status === 0 && !slot.presentUsers.has(uid)) {
          slot.absentUsers.add(uid);
        }
      }
      bySlot.set(slotKey, slot);

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

    const dailyTrend = [...bySlot.values()]
      .map((slot) => {
        const present = slot.presentUsers.size;
        const absent = slot.absentUsers.size;
        return {
          date: slot.date,
          period: slot.period,
          present,
          absent,
          /** 应到：班级花名册总人数 */
          total: expected,
          rate: expected > 0 ? Math.round((present / expected) * 1000) / 10 : 0,
        };
      })
      .sort((a, b) => {
        const dc = b.date.localeCompare(a.date);
        if (dc !== 0) return dc;
        return PERIOD_SORT[a.period] - PERIOD_SORT[b.period];
      })
      .slice(0, 28);

    let sumPresent = 0;
    let sumExpected = 0;
    const dates = new Set<string>();
    for (const row of dailyTrend) {
      sumPresent += row.present;
      sumExpected += row.total;
      dates.add(row.date);
    }
    // 累计出勤率：各时段「实到÷应到」合计（与表格口径一致）
    const cumulativeRate =
      sumExpected > 0 ? Math.round((sumPresent / sumExpected) * 1000) / 10 : 0;

    const warnings = [...byUser.entries()]
      .map(([userId, u]) => {
        const rate = u.total > 0 ? Math.round((u.present / u.total) * 1000) / 10 : 100;
        return { userId, ...u, rate };
      })
      .filter((u) => u.absent >= 2 || u.rate < 80)
      .sort((a, b) => b.absent - a.absent || a.rate - b.rate)
      .slice(0, 20);

    return {
      recordDays: dates.size,
      recordCount: rows.length,
      cumulativeRate,
      dailyTrend,
      loading: allAttendanceLoading,
      warnings,
    };
  }, [allAttendancePage?.items, allStudents, allAttendanceLoading, rosterTotal, scheduleList]);

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
      queryClient.invalidateQueries({ queryKey: ['follow-class-hours', classIdNum] });
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
      queryClient.invalidateQueries({ queryKey: ['follow-class-hours', classIdNum] });
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
      queryClient.invalidateQueries({ queryKey: ['follow-class-hours', classIdNum] });
      queryClient.invalidateQueries({ queryKey: ['follow-quality-metrics', classIdNum] });
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
    {
      title: '姓名',
      dataIndex: 'realname',
      width: 120,
      ellipsis: true,
      render: (v?: string, row?: ClassStudent) => {
        const name = (v || row?.realname || '').trim();
        if (name) return name;
        if (row?.phone) return row.phone;
        if (row?.id_card) return row.id_card;
        return '—';
      },
    },
    { title: '手机', dataIndex: 'phone', width: 130, render: (v?: string) => v || '—' },
    {
      title: '考勤状态',
      width: 168,
      render: (_, row) => {
        const att = attendanceMap.get(String(row.id));
        const periodLabels = attendancePeriodLabelsByUser.get(String(row.id)) ?? [];
        if ((!att || att.status == null) && periodLabels.length === 0) {
          return <Tag>未登记</Tag>;
        }
        const meta =
          att?.status != null
            ? ATTENDANCE_STATUS[att.status] ?? { text: String(att.status), color: 'default' }
            : null;
        return (
          <Space direction="vertical" size={2} style={{ lineHeight: 1.3 }}>
            {meta ? <Tag color={meta.color}>{meta.text}</Tag> : <Tag>未登记</Tag>}
            {periodLabels.map((label) => (
              <Typography.Text key={label} type="secondary" style={{ fontSize: 12 }}>
                {label}
              </Typography.Text>
            ))}
          </Space>
        );
      },
    },
    {
      title: '签到时间',
      width: 110,
      render: (_, row) => attendanceMap.get(String(row.id))?.checkInTime || '—',
    },
    {
      title: '已上课时',
      width: 100,
      render: (_, row) => {
        const hours = hoursByPeopleId.get(String(row.id));
        if (!hours || hours.attendedHours == null) return '—';
        return formatClassHours(hours.attendedHours);
      },
    },
    {
      title: '完成率',
      width: 90,
      render: (_, row) => {
        const hours = hoursByPeopleId.get(String(row.id));
        if (!hours || hours.completionRate == null) return '—';
        return `${Number(hours.completionRate).toFixed(1)}%`;
      },
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

  const followLogCountQuery = useQuery({
    queryKey: ['follow-op-logs', classIdNum, 'overview-count'],
    queryFn: () => trainApi.listFollowOpLogs(classIdNum!),
    enabled: classIdNum != null && classIdNum > 0,
  });

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
              options={sessionSelectOptions}
            />
          </span>
          <Tag>
            {hasBizDate
              ? isSessionAttendance
                ? '课次考勤'
                : scheduleItems.length
                  ? `按日汇总 · 当日 ${scheduleItems.length} 课次`
                  : sessionSelectOptions.length > 1
                    ? `按日汇总 · 考勤课次 ${sessionSelectOptions.length - 1} 个`
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
                    <Card size="small"><Statistic title="当日未登记" value={dayStats.unregistered} /></Card>
                  </Col>
                  <Col xs={12} sm={8} md={4}>
                    <Card
                      size="small"
                      hoverable
                      onClick={() => setActiveTab('logs')}
                      style={{ cursor: 'pointer' }}
                    >
                      <Statistic
                        title="跟班日志"
                        value={followLogCountQuery.data?.length ?? 0}
                        prefix={<HistoryOutlined />}
                        loading={followLogCountQuery.isLoading}
                      />
                    </Card>
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
                          rowKey={(r) => `${r.date}|${r.period}`}
                          pagination={false}
                          sticky
                          dataSource={classStats.dailyTrend}
                          columns={[
                            { title: '日期', dataIndex: 'date', width: 110 },
                            {
                              title: '时段',
                              dataIndex: 'period',
                              width: 64,
                              render: (v: string) => <Tag>{v}</Tag>,
                            },
                            {
                              title: '实到/应到',
                              width: 100,
                              render: (_, r) => `${r.present}/${r.total}`,
                            },
                            {
                              title: '缺勤',
                              dataIndex: 'absent',
                              width: 64,
                            },
                            {
                              title: '出勤率',
                              dataIndex: 'rate',
                              render: (v: number) => (
                                <Space>
                                  <Progress
                                    percent={v}
                                    size="small"
                                    style={{ width: 72, margin: 0 }}
                                    status={v < 80 ? 'exception' : 'normal'}
                                  />
                                  <span>{v}%</span>
                                </Space>
                              ),
                            },
                            {
                              title: '',
                              width: 64,
                              render: (_, r) => (
                                <Button
                                  type="link"
                                  size="small"
                                  style={{ padding: 0 }}
                                  onClick={() => {
                                    setPendingPeriodNav({ date: r.date, period: r.period });
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
                            title="计划学时"
                            value={formatClassHours(classHoursData?.plannedHours ?? scheduleStats.totalHours)}
                            suffix="课时"
                          />
                        </Col>
                        <Col xs={12} sm={8} md={4}>
                          <Statistic title="知识点" value={scheduleStats.subjectCount} suffix="个" />
                        </Col>
                        <Col xs={12} sm={8} md={4}>
                          <Statistic title="上课天数" value={scheduleStats.dayCount} suffix="天" />
                        </Col>
                        <Col xs={12} sm={8} md={4}>
                          <Statistic title="授课老师" value={scheduleStats.teacherCount} suffix="人" />
                        </Col>
                        <Col xs={12} sm={8} md={4}>
                          <Statistic title="今日课次" value={scheduleStats.todayCount} suffix="节" />
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
                          {(classHoursData?.unconvertibleSessions ?? 0) > 0 ? (
                            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                              有 {classHoursData?.unconvertibleSessions} 节无法折算（无时间也无学时），不计入计划/已上课时
                            </Typography.Text>
                          ) : scheduleStats.hoursKnown < scheduleStats.sessionCount ? (
                            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                              {scheduleStats.hoursKnown === 0
                                ? '部分课次未填学时；有起止时间的已按 45 分钟折算计划学时'
                                : `有 ${scheduleStats.sessionCount - scheduleStats.hoursKnown} 节未填学时，有时间的已按 45 分钟折算`}
                            </Typography.Text>
                          ) : (
                            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                              计划学时按课表起止时间 ÷ 45 分钟；无时间则用课表学时
                            </Typography.Text>
                          )}
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
                        <div>教务人员：{[...staffList, ...chargeList].map((s) => s.displayName).join('、') || '未设置'}</div>
                        <div>理论老师：{classRoles.filter((r) => r.roleCode === 'lead_teacher').map((s) => s.displayName).join('、') || '未设置'}</div>
                        <div>实操老师：{classRoles.filter((r) => r.roleCode === 'assistant').map((s) => s.displayName).join('、') || '未设置'}</div>
                        <Typography.Text type="secondary">角色在开班准备「班级角色」与本页同步维护。</Typography.Text>
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
                  <AttendanceExportButton classId={classIdNum} studentTotal={studentTotal} />
                  <Typography.Text type="secondary">
                    支持人社/培训平台「考勤信息列表」.xls/.xlsx；按身份证匹配本班学员，有上课/下课时间时按课次行写入（匹配课表，必要时自动补建课次），并按「是否旷课」计课时。无时段时仍合并为按日记录。
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
                  scroll={{ x: 920 }}
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
                  title={
                    <Space size="middle" wrap>
                      <Button
                        type="link"
                        style={{
                          padding: 0,
                          height: 'auto',
                          fontWeight: schedulePanel === 'list' ? 600 : 400,
                          color: schedulePanel === 'list' ? undefined : 'rgba(0,0,0,0.45)',
                        }}
                        onClick={() => setSchedulePanel('list')}
                      >
                        班级课程表
                      </Button>
                      <Button
                        type="link"
                        style={{
                          padding: 0,
                          height: 'auto',
                          fontWeight: schedulePanel === 'teacherHours' ? 600 : 400,
                          color: schedulePanel === 'teacherHours' ? undefined : 'rgba(0,0,0,0.65)',
                        }}
                        onClick={() => setSchedulePanel('teacherHours')}
                      >
                        教师上课时长
                      </Button>
                    </Space>
                  }
                  style={{ marginBottom: 16 }}
                  loading={
                    schedulePanel === 'list'
                      ? scheduleLoading || importKcbiaoMu.isPending || saveScheduleMu.isPending
                      : false
                  }
                  extra={
                    schedulePanel === 'teacherHours' ? (
                      <Space wrap size="small">
                        <Button
                          size="small"
                          icon={<ExportOutlined />}
                          loading={exportTeacherDaysMu.isPending}
                          disabled={(teacherDaysData?.teachers?.length ?? 0) === 0}
                          onClick={() => exportTeacherDaysMu.mutate()}
                        >
                          导出课酬表
                        </Button>
                        {saveTeacherPayMu.isPending ? (
                          <Typography.Text type="secondary">保存中…</Typography.Text>
                        ) : null}
                        <Button size="small" onClick={() => setSchedulePanel('list')}>
                          返回课表
                        </Button>
                      </Space>
                    ) : (
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
                        onClick={() => saveScheduleMu.mutate(undefined)}
                      >
                        保存课表
                      </Button>
                    </Space>
                    )
                  }
                >
                  {schedulePanel === 'teacherHours' ? (
                    <Space direction="vertical" size={16} style={{ width: '100%' }}>
                      <Alert
                        type="info"
                        showIcon
                        message="天数按本班课表现算（1 课次 = 0.5 天，同日最多 2 节）；课酬为本班单价，修改后自动保存；合计 = 已上天数 × 单价（四舍五入到元）。按老师×授课形式分行。"
                      />
                      <Row gutter={[12, 12]}>
                        <Col xs={12} sm={8} md={4}>
                          <Statistic
                            title="已上天数合计"
                            value={teacherDaysData?.taughtDaysSum ?? '—'}
                            suffix="天"
                            loading={teacherDaysLoading}
                          />
                        </Col>
                        <Col xs={12} sm={8} md={4}>
                          <Statistic
                            title="计划天数合计"
                            value={teacherDaysData?.plannedDaysSum ?? '—'}
                            suffix="天"
                            loading={teacherDaysLoading}
                          />
                        </Col>
                        <Col xs={12} sm={8} md={4}>
                          <Statistic
                            title="课酬合计"
                            value={teacherPayDisplaySum ?? '—'}
                            suffix="元"
                            loading={teacherDaysLoading}
                          />
                        </Col>
                        <Col xs={12} sm={8} md={4}>
                          <Statistic
                            title="未指派老师课次"
                            value={teacherDaysData?.unassignedSessionCount ?? 0}
                            loading={teacherDaysLoading}
                          />
                        </Col>
                        <Col xs={12} sm={8} md={4}>
                          <Statistic
                            title="超限课次"
                            value={teacherDaysData?.overflowSessionCount ?? 0}
                            valueStyle={
                              (teacherDaysData?.overflowSessionCount ?? 0) > 0
                                ? { color: '#d48806' }
                                : undefined
                            }
                            loading={teacherDaysLoading}
                          />
                        </Col>
                      </Row>
                      {(teacherDaysData?.teachers?.length ?? 0) === 0 && !teacherDaysLoading ? (
                        <Empty
                          image={Empty.PRESENTED_IMAGE_SIMPLE}
                          description="本班课次尚未指派老师，请回课表补充「老师」"
                        >
                          <Button type="primary" onClick={() => setSchedulePanel('list')}>
                            返回课表
                          </Button>
                        </Empty>
                      ) : (
                        <Table
                          size="small"
                          rowKey={(r) => `${r.teacherName}|${r.teachForm ?? '未填'}`}
                          loading={teacherDaysLoading}
                          pagination={false}
                          dataSource={teacherDaysData?.teachers ?? []}
                columns={[
                            { title: '授课老师', dataIndex: 'teacherName', ellipsis: true, width: 120 },
                            {
                              title: '授课形式',
                              dataIndex: 'teachForm',
                              width: 88,
                              render: (v?: string) => v || '未填',
                            },
                            {
                              title: '已上课次',
                              dataIndex: 'taughtSessionCount',
                              width: 96,
                              align: 'right',
                            },
                            {
                              title: '已上天数',
                              dataIndex: 'taughtDays',
                              width: 96,
                              align: 'right',
                              render: (v?: number | null) =>
                                v == null ? '—' : Number(v).toFixed(1),
                            },
                            {
                              title: '计划课次',
                              dataIndex: 'plannedSessionCount',
                              width: 96,
                              align: 'right',
                            },
                            {
                              title: '计划天数',
                              dataIndex: 'plannedDays',
                              width: 96,
                              align: 'right',
                              render: (v?: number | null) =>
                                v == null ? '—' : Number(v).toFixed(1),
                            },
                            {
                              title: '超限',
                              dataIndex: 'overflowSessionCount',
                              width: 72,
                              align: 'right',
                              render: (v?: number) =>
                                (v ?? 0) > 0 ? <Tag color="warning">{v}</Tag> : '—',
                            },
                            {
                              title: '课酬（元/天）',
                              dataIndex: 'ratePerDay',
                              width: 140,
                              render: (_: unknown, row: trainApi.FollowTeacherDayRow) => {
                                const key = `${row.teacherName}|${row.teachForm ?? '未填'}`;
                                const draft = teacherPayDraft[key];
                                return (
                                  <InputNumber
                                    size="small"
                                    min={0}
                                    precision={2}
                                    style={{ width: '100%' }}
                                    value={draft?.ratePerDay ?? undefined}
                                    placeholder="未设"
                                    onChange={(val) => {
                                      const rate = val == null ? null : Number(val);
                                      const next = {
                                        ...teacherPayDraftRef.current,
                                        [key]: {
                                          ...teacherPayDraftRef.current[key],
                                          ratePerDay: rate,
                                        },
                                      };
                                      teacherPayDraftRef.current = next;
                                      setTeacherPayDraft(next);
                                      scheduleSaveTeacherPay(next);
                                    }}
                                    onBlur={() => {
                                      if (teacherPaySaveTimerRef.current) {
                                        flushSaveTeacherPay();
                                      }
                                    }}
                                  />
                                );
                              },
                            },
                            {
                              title: '合计课酬',
                              key: 'payTotal',
                              width: 100,
                              align: 'right',
                              render: (_: unknown, row: trainApi.FollowTeacherDayRow) => {
                                const key = `${row.teacherName}|${row.teachForm ?? '未填'}`;
                                const draft = teacherPayDraft[key];
                                const rate = draft?.ratePerDay ?? row.ratePerDay;
                                const days = row.taughtDays;
                                if (rate == null || days == null) return '—';
                                return Math.round(Number(rate) * Number(days));
                              },
                            },
                           
                          ]}
                        />
                      )}
                    </Space>
                  ) : (
                  <>
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
                              <Popconfirm
                                title="删除该课次？"
                                onConfirm={() => removeScheduleRow(r._key)}
                              >
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
                  </>
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
            key: 'review-sync',
            label: '同步学',
            children: (
              <FollowReviewSyncTab
                classId={classId}
                classTitle={classItem.title}
                active={activeTab === 'review-sync'}
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
                    <Card size="small" loading={qualityMetricsLoading}>
                      <Statistic
                        title="计划学时"
                        value={formatClassHours(qualityMetrics?.plannedHours)}
                        suffix="课时"
                      />
                    </Card>
                  </Col>
                  <Col xs={12} md={6}>
                    <Card size="small" loading={qualityMetricsLoading}>
                      <Statistic
                        title="已上课时（人均）"
                        value={formatClassHours(qualityMetrics?.attendedHoursAvg)}
                        suffix="课时"
                      />
                    </Card>
                  </Col>
                  <Col xs={12} md={6}>
                    <Card size="small" loading={qualityMetricsLoading}>
                      <Statistic
                        title="课时完成率"
                        value={qualityMetrics?.hoursCompletionAvg ?? '—'}
                        suffix={qualityMetrics?.hoursCompletionAvg != null ? '%' : undefined}
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
                          qualityMetrics?.evidencePhotoCount != null
                            ? Number(qualityMetrics.evidencePhotoCount)
                            : qualityMetrics?.evidenceReady
                              ? 1
                              : 0
                        }
                        suffix="张"
                      />
                      <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                        {qualityMetrics?.evidenceReady ? '可生成培训照片' : '请在留痕页手动添加'}
                      </Typography.Text>
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
                      <Button type="link" size="small" style={{ padding: 0, marginTop: 4 }} onClick={() => void openClassSubsidy()}>
                        {qualityMetrics?.subsidyId == null ? '为本班申报补贴' : '办理本班补贴'}
                      </Button>
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
                      个有考勤日）。计划学时=课表起止时间÷45 分钟（无时间用课表学时）。已上课时=今日及以前课次中未旷课的课时之和（未来课次不计）；未登记视为未旷课。课时完成率=已上课时/全课表计划学时。作业完成率=已交有效份数÷应发份数。模拟通过率=已评通过份数÷已评份数。考试通过率=得分≥及格线人数÷有成绩人数。满意度=答卷均分（1～5）。补贴齐套=配方必填槽位均已有材料（或已生成材料包）。
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
        hoursRow={detailStudent ? hoursByPeopleId.get(String(detailStudent.id)) : undefined}
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
                pagination={{ pageSize: 8, showSizeChanger: false }}
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
        onOk={() => submitScheduleEditor()}
        confirmLoading={saveScheduleMu.isPending}
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
            <TeacherBankSelect />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

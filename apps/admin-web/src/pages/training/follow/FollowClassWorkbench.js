import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useDeferredValue, useEffect, useMemo, useRef, useState } from 'react';
import { Alert, Button, Card, Col, DatePicker, Empty, Form, Input, InputNumber, Modal, Popconfirm, Progress, Row, Select, Space, Statistic, Table, Tabs, Tag, TimePicker, Typography, Upload, message, } from 'antd';
import { ArrowLeftOutlined, CheckCircleOutlined, DeleteOutlined, EditOutlined, ExportOutlined, HistoryOutlined, LineChartOutlined, PlusOutlined, ReloadOutlined, StarOutlined, TeamOutlined, UploadOutlined, } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { downloadUrlFile } from '../../../api/files';
import * as trainApi from '../../../api/training';
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
const ATTENDANCE_STATUS = {
    0: { text: '缺勤', color: 'error' },
    1: { text: '出勤', color: 'success' },
    2: { text: '迟到', color: 'warning' },
    3: { text: '早退', color: 'orange' },
};
function toNumId(id) {
    const n = typeof id === 'number' ? id : Number(id);
    return Number.isFinite(n) ? n : null;
}
function formatClassHours(value) {
    if (value == null || value === '')
        return '—';
    const n = typeof value === 'number' ? value : Number(value);
    if (!Number.isFinite(n))
        return '—';
    return n.toFixed(1);
}
/**
 * 从「HH:mm」或「yyyy-MM-dd HH:mm:ss」等串中取小时。
 * 注意：不可用 ^(\\d{1,2}) 直接匹配，否则会把日期里的「2026」误读成 20 点 → 全变成下午。
 */
function parseHour(time) {
    const raw = String(time ?? '').trim();
    if (!raw)
        return null;
    // 优先匹配时分：空格/T 后，或整串就是 HH:mm[:ss]
    const m = raw.match(/(?:[T\s]|^\s*)(\d{1,2}):(\d{2})(?::\d{2})?/)
        ?? raw.match(/^(\d{1,2}):(\d{2})(?::\d{2})?$/);
    if (!m)
        return null;
    const h = Number(m[1]);
    return Number.isFinite(h) && h >= 0 && h <= 23 ? h : null;
}
/** 无法判断时段时返回 null（不归入「全天」） */
function periodFromTime(time) {
    const h = parseHour(time);
    if (h == null)
        return null;
    return h < 12 ? '上午' : '下午';
}
/** 从导入备注「导入课次：08:30-11:30」取上课开始时间 */
function startTimeFromAttendanceRemark(remark) {
    const r = String(remark ?? '');
    const m = r.match(/导入课次：\s*(\d{1,2}:\d{2})/);
    return m ? m[1] : null;
}
/** 从导入备注解析起止时间 */
function sessionTimesFromAttendanceRemark(remark) {
    const r = String(remark ?? '');
    const m = r.match(/导入课次：\s*(\d{1,2}:\d{2})\s*[-–—~～]\s*(\d{1,2}:\d{2})/);
    if (m)
        return { start: m[1], end: m[2] };
    return { start: startTimeFromAttendanceRemark(remark), end: null };
}
/** 统一成 YYYY-MM-DD，兼容带时间 / 斜杠日期 */
function normalizeBizDate(raw) {
    const s = String(raw ?? '').trim();
    if (!s)
        return '';
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
function extractHm(time) {
    const raw = String(time ?? '').trim();
    if (!raw)
        return null;
    const m = raw.match(/(?:[T\s]|^)(\d{1,2}):(\d{2})(?::\d{2})?/);
    if (!m)
        return null;
    return `${m[1].padStart(2, '0')}:${m[2]}`;
}
function formatSessionLabel(startTime, endTime, fallback) {
    const start = extractHm(startTime);
    const end = extractHm(endTime);
    if (!start && !end)
        return fallback || '课次';
    return `${start || '—'}-${end || '—'}`;
}
/** 出勤优先：出勤 > 迟到 > 早退 > 缺勤 */
function attendanceStatusRank(status) {
    if (status === 1)
        return 4;
    if (status === 2)
        return 3;
    if (status === 3)
        return 2;
    if (status === 0)
        return 1;
    return 0;
}
/** 同日多课次合并为一条展示（任一有效出勤即出勤；签到时间拼接） */
function mergeDayAttendanceRows(list) {
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
        checkInTime: uniqueCheckIns.length > 1 ? uniqueCheckIns.join(' / ') : uniqueCheckIns[0] ?? best.checkInTime,
        remark: uniqueRemarks.length > 1 ? uniqueRemarks.join('；') : uniqueRemarks[0] ?? best.remark,
    };
}
/** 从考勤行解析「上午 08:30-11:30」类标注 */
function resolveSessionPeriodLabel(row, schedules) {
    const sch = row.scheduleId != null
        ? schedules.find((s) => String(s.id) === String(row.scheduleId))
        : undefined;
    const fromRemark = sessionTimesFromAttendanceRemark(row.remark);
    const start = (sch?.startTime != null ? String(sch.startTime) : null)
        || fromRemark.start
        || extractHm(row.checkInTime);
    const end = (sch?.endTime != null ? String(sch.endTime) : null)
        || fromRemark.end
        || null;
    const period = periodFromTime(start)
        || periodFromTime(fromRemark.start)
        || periodFromTime(row.checkInTime);
    if (!period)
        return null;
    const timeLabel = formatSessionLabel(start, end, '');
    return {
        period,
        timeLabel: timeLabel && timeLabel !== '课次' ? timeLabel : '',
        sortKey: scheduleStartSortKey(start),
    };
}
const PERIOD_SORT = { 上午: 0, 下午: 1 };
function scheduleStartSortKey(startTime) {
    const h = parseHour(startTime);
    const m = String(startTime ?? '').match(/:(\d{2})/);
    const mm = m ? m[1] : '00';
    if (h == null)
        return '99:99';
    return `${String(h).padStart(2, '0')}:${mm}`;
}
/** 按日期+上午/下午挑选课次；上午取最早一节，下午取最晚一节 */
function pickScheduleForPeriod(schedules, date, period, preferScheduleIds) {
    const candidates = schedules.filter((s) => {
        if (normalizeBizDate(s.scheduleDate != null ? String(s.scheduleDate) : null) !== date)
            return false;
        return periodFromTime(s.startTime != null ? String(s.startTime) : null) === period;
    });
    if (candidates.length === 0)
        return null;
    const preferred = preferScheduleIds && preferScheduleIds.size > 0
        ? candidates.filter((s) => s.id != null && preferScheduleIds.has(String(s.id)))
        : [];
    const pool = preferred.length > 0 ? preferred : candidates;
    pool.sort((a, b) => scheduleStartSortKey(a.startTime != null ? String(a.startTime) : null).localeCompare(scheduleStartSortKey(b.startTime != null ? String(b.startTime) : null)));
    return period === '下午' ? pool[pool.length - 1] : pool[0];
}
/** 师资库下拉：可搜索、可选手动输入姓名（antd Select 搜索不自动提交，用 onSearch/onBlur 提交）。 */
function TeacherBankSelect({ value, onChange }) {
    const [searchText, setSearchText] = useState('');
    const deferredKeyword = useDeferredValue(searchText.trim());
    const { data, isFetching } = useQuery({
        queryKey: ['teacher-banks-pick', deferredKeyword],
        queryFn: () => trainApi.pageTeacherBanks({
            page: 1,
            pageSize: 50,
            keyword: deferredKeyword || undefined,
        }),
    });
    const bankOptions = useMemo(() => {
        const items = data?.items ?? [];
        const seen = new Set();
        const opts = [];
        for (const t of items) {
            const name = String(t.name ?? '').trim();
            if (!name || seen.has(name))
                continue;
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
    const commit = (raw) => {
        const next = raw?.trim() || undefined;
        if (next !== value)
            onChange?.(next);
    };
    return (_jsx(Select, { showSearch: true, allowClear: true, placeholder: "\u4ECE\u5E08\u8D44\u5E93\u9009\u62E9\u6216\u624B\u52A8\u8F93\u5165", loading: isFetching, value: value || undefined, searchValue: searchText, options: options, filterOption: false, notFoundContent: isFetching ? '加载中…' : '无匹配，回车或失焦可手动录入', onSearch: setSearchText, onChange: (v) => {
            setSearchText('');
            commit(v);
        }, onBlur: () => {
            if (searchText.trim() && searchText.trim() !== value)
                commit(searchText);
        }, onInputKeyDown: (e) => {
            if (e.key === 'Enter' && searchText.trim() && searchText.trim() !== value) {
                e.preventDefault();
                commit(searchText);
                setSearchText('');
            }
        }, style: { width: '100%' } }));
}
/** 分页拉齐考勤，避免单页 5000 截断导致累计统计不准 */
async function fetchAllAttendances(params) {
    const pageSize = 500;
    const maxPages = 200;
    const items = [];
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
        if (batch.length === 0)
            break;
        page += 1;
    }
    if (items.length < total) {
        message.warning(`考勤记录较多，仅加载 ${items.length}/${total} 条，累计统计可能偏少`);
    }
    return { items, total };
}
export function FollowClassWorkbench({ classItem, onBack }) {
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
        }
        catch (e) {
            const msg = e.message || '打开补贴失败';
            message.error(msg);
            trainApi.stashSubsidyDeclareFocus({ classId: classIdNum, openCreate: true });
            navigateTo('train_subsidy_declare');
        }
    };
    const [activeTab, setActiveTab] = useState('overview');
    const [bizDate, setBizDate] = useState(dayjs());
    const [sessionKey, setSessionKey] = useState('day');
    /** 概览「查看」：先改日期，待课表选项就绪后再切到对应上下午课次 */
    const [pendingPeriodNav, setPendingPeriodNav] = useState(null);
    const [studentPage, setStudentPage] = useState(1);
    const [detailStudent, setDetailStudent] = useState(null);
    const [importResult, setImportResult] = useState(null);
    const [scheduleDraft, setScheduleDraft] = useState([]);
    const [scheduleDirty, setScheduleDirty] = useState(false);
    const [scheduleModalOpen, setScheduleModalOpen] = useState(false);
    const [editingScheduleKey, setEditingScheduleKey] = useState(null);
    const [scheduleForm] = Form.useForm();
    /** 课程表 Tab：课表列表 | 教师上课时长 */
    const [schedulePanel, setSchedulePanel] = useState('list');
    /** 本班课酬编辑草稿：key = teacherName|teachForm */
    const [teacherPayDraft, setTeacherPayDraft] = useState({});
    const teacherPayDraftRef = useRef(teacherPayDraft);
    teacherPayDraftRef.current = teacherPayDraft;
    const teacherPaySaveTimerRef = useRef(null);
    const teacherPaySavingRef = useRef(false);
    const dateStr = bizDate ? bizDate.format('YYYY-MM-DD') : '';
    const hasBizDate = Boolean(dateStr);
    /** 课次跟班：sessionKey 为课次 id；day=按日（schedule_id 空） */
    const selectedScheduleId = useMemo(() => {
        if (sessionKey === 'day')
            return null;
        const n = Number(sessionKey);
        return Number.isFinite(n) && n > 0 ? n : null;
    }, [sessionKey]);
    const isSessionAttendance = selectedScheduleId != null;
    const { data: scheduleStatus, refetch: refetchScheduleStatus } = useQuery({
        queryKey: ['follow-schedule-status', classIdNum],
        queryFn: () => trainApi.getClassScheduleStatus(classIdNum),
        enabled: classIdNum != null && classIdNum > 0,
    });
    const hasKcbiaoFile = Boolean(scheduleStatus?.hasKcbiaoFile || classItem.kcbiao_file || classItem.kcbiao_src);
    const scheduleManualEdited = Boolean(scheduleStatus?.manualEdited);
    const { data: scheduleData, isLoading: scheduleLoading } = useQuery({
        queryKey: ['follow-schedule', classIdNum],
        queryFn: () => trainApi.getClassSchedule(classIdNum),
        enabled: classIdNum != null && classIdNum > 0,
    });
    const { data: pendingChangeRequests } = useQuery({
        queryKey: ['schedule-change-requests', classIdNum, 'pending'],
        queryFn: () => trainApi.listScheduleChangeRequests({ classId: classIdNum, status: 'pending' }),
        enabled: classIdNum != null && classIdNum > 0,
    });
    const pendingChangeCount = pendingChangeRequests?.length ?? 0;
    const { data: practiceScope, isLoading: practiceScopeLoading } = useQuery({
        queryKey: ['class-practice-scope', classId],
        queryFn: () => trainApi.getClassPracticeScope(classId),
        enabled: !!classId,
    });
    useEffect(() => {
        if (scheduleDirty)
            return;
        const list = (scheduleData ?? []).map((row, idx) => ({
            ...row,
            _key: String(row.id ?? `tmp-${idx}-${row.scheduleDate}-${row.startTime}`),
        }));
        list.sort((a, b) => {
            const da = String(a.scheduleDate ?? '');
            const db = String(b.scheduleDate ?? '');
            if (da !== db)
                return da.localeCompare(db);
            return String(a.startTime ?? '').localeCompare(String(b.startTime ?? ''));
        });
        setScheduleDraft(list);
    }, [scheduleData, scheduleDirty]);
    const scheduleList = scheduleDraft;
    const scheduleItems = useMemo(() => {
        return scheduleList.filter((s) => normalizeBizDate(s.scheduleDate != null ? String(s.scheduleDate) : null) === dateStr);
    }, [scheduleList, dateStr]);
    /** 概览课表摘要：节次 / 学时合计 / 科目数 / 授课形式 / 上课天数 */
    const scheduleStats = useMemo(() => {
        const subjects = new Set();
        const teachers = new Set();
        const days = new Set();
        let totalHours = 0;
        let hoursKnown = 0;
        let theoryCount = 0;
        let practiceCount = 0;
        for (const row of scheduleList) {
            const name = String(row.courseName ?? '').trim();
            if (name)
                subjects.add(name);
            const teacher = String(row.teacherName ?? '').trim();
            if (teacher)
                teachers.add(teacher);
            const day = String(row.scheduleDate ?? '').trim();
            if (day)
                days.add(day);
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
            }
            else if (form.includes('理论')) {
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
    const dayOfWeekLabel = (v) => {
        const n = typeof v === 'number' ? v : Number(v);
        return ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日'][n] ?? '';
    };
    const saveScheduleMu = useMutation({
        mutationFn: (items) => {
            if (classIdNum == null)
                return Promise.reject(new Error('无效班级'));
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
        onError: (e) => message.error(e.message || '保存课表失败'),
    });
    const importKcbiaoMu = useMutation({
        mutationFn: (opts) => {
            if (classIdNum == null)
                return Promise.reject(new Error('无效班级'));
            return trainApi.importClassScheduleFromKcbiao(classIdNum, opts.force, opts.confirmOverwriteManual ?? false);
        },
        onSuccess: (res) => {
            if (res.skipped) {
                message.info(res.reason || '已有课表，未覆盖');
            }
            else {
                message.success(`已从开班课表导入 ${res.imported ?? res.count ?? 0} 节`);
            }
            setScheduleDirty(false);
            queryClient.invalidateQueries({ queryKey: ['follow-schedule', classIdNum] });
            queryClient.invalidateQueries({ queryKey: ['follow-class-hours', classIdNum] });
            queryClient.invalidateQueries({ queryKey: ['follow-teacher-days', classIdNum] });
            queryClient.invalidateQueries({ queryKey: ['follow-quality-metrics', classIdNum] });
            void refetchScheduleStatus();
        },
        onError: (e) => message.error(e.message || '导入开班课表失败'),
    });
    const exportScheduleMu = useMutation({
        mutationFn: () => {
            if (classIdNum == null)
                return Promise.reject(new Error('无效班级'));
            return trainApi.exportClassSchedule(classIdNum);
        },
        onSuccess: async (data) => {
            if (data.downUrl) {
                await downloadUrlFile(data.downUrl, data.fileName || '课表.csv');
            }
            message.success('课表已导出');
        },
        onError: (e) => message.error(e.message || '导出课表失败'),
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
    const runImportKcbiao = (force) => {
        if (force && scheduleManualEdited) {
            Modal.confirm({
                title: '确认覆盖已调课课表？',
                content: '本班课表已在跟班手工调整过。用开班 Excel 重导将覆盖当前结构化课次，开班原文件不会被改写。',
                okText: '确认覆盖',
                okButtonProps: { danger: true },
                onOk: () => importKcbiaoMu.mutateAsync({ force: true, confirmOverwriteManual: true }),
            });
            return;
        }
        importKcbiaoMu.mutate({ force, confirmOverwriteManual: false });
    };
    const openScheduleEditor = (row) => {
        if (row) {
            setEditingScheduleKey(row._key);
            scheduleForm.setFieldsValue({
                scheduleDate: row.scheduleDate ? dayjs(String(row.scheduleDate)) : undefined,
                courseName: row.courseName,
                classHours: row.classHours,
                teachForm: row.teachForm,
                teacherName: row.teacherName,
                timeRange: row.startTime && row.endTime
                    ? [dayjs(String(row.startTime), 'HH:mm'), dayjs(String(row.endTime), 'HH:mm')]
                    : undefined,
            });
        }
        else {
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
        const scheduleDate = v.scheduleDate.format('YYYY-MM-DD');
        const startTime = v.timeRange[0].format('HH:mm');
        const endTime = v.timeRange[1].format('HH:mm');
        const dayOfWeek = dayjs(scheduleDate).day() === 0 ? 7 : dayjs(scheduleDate).day();
        const next = {
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
    const removeScheduleRow = async (key) => {
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
            const list = [];
            let page = 1;
            let total = Number.POSITIVE_INFINITY;
            const maxPages = 100;
            while (list.length < total && page <= maxPages) {
                const res = await trainApi.listClassStudents(classId, page, pageSize);
                const batch = res.list ?? [];
                list.push(...batch);
                total = typeof res.total === 'number' ? res.total : list.length;
                if (batch.length === 0)
                    break;
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
        queryFn: () => fetchAllAttendances({
            classId: classIdNum,
            attendanceDate: dateStr,
            ...(isSessionAttendance ? { scheduleId: selectedScheduleId } : {}),
        }),
        enabled: classIdNum != null && classIdNum > 0 && hasBizDate,
        retry: false,
    });
    /** 班级全部考勤（累计统计 / 近日出勤） */
    const { data: allAttendancePage, isLoading: allAttendanceLoading } = useQuery({
        queryKey: ['follow-attendance-all', classIdNum],
        queryFn: () => fetchAllAttendances({ classId: classIdNum }),
        enabled: classIdNum != null && classIdNum > 0,
        retry: false,
    });
    /** 近日出勤率「查看」：业务日落地后再绑定上午/下午课次，避免仍停在按日或上午 */
    useEffect(() => {
        if (!pendingPeriodNav)
            return;
        if (dateStr !== pendingPeriodNav.date)
            return;
        const preferIds = new Set();
        for (const row of allAttendancePage?.items ?? []) {
            if (normalizeBizDate(row.attendanceDate) !== pendingPeriodNav.date)
                continue;
            if (row.scheduleId == null)
                continue;
            const sch = scheduleList.find((s) => String(s.id) === String(row.scheduleId));
            const period = periodFromTime(sch?.startTime != null ? String(sch.startTime) : null)
                || periodFromTime(startTimeFromAttendanceRemark(row.remark))
                || periodFromTime(row.checkInTime);
            if (period === pendingPeriodNav.period) {
                preferIds.add(String(row.scheduleId));
            }
        }
        const pick = pickScheduleForPeriod(scheduleList, pendingPeriodNav.date, pendingPeriodNav.period, preferIds);
        if (pick?.id != null) {
            setSessionKey(String(pick.id));
            setPendingPeriodNav(null);
            return;
        }
        if (preferIds.size > 0) {
            const ids = [...preferIds].sort((a, b) => {
                const sa = scheduleList.find((s) => String(s.id) === a);
                const sb = scheduleList.find((s) => String(s.id) === b);
                return scheduleStartSortKey(sa?.startTime != null ? String(sa.startTime) : null).localeCompare(scheduleStartSortKey(sb?.startTime != null ? String(sb.startTime) : null));
            });
            const id = pendingPeriodNav.period === '下午' ? ids[ids.length - 1] : ids[0];
            setSessionKey(id);
            setPendingPeriodNav(null);
            return;
        }
        if (scheduleLoading)
            return;
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
        const byId = new Map();
        const putSession = (id, start, end, fallback) => {
            if (!id || byId.has(id))
                return;
            const label = formatSessionLabel(start, end, fallback || `课次#${id}`);
            byId.set(id, {
                value: id,
                label,
                sortKey: scheduleStartSortKey(start),
            });
        };
        for (const s of scheduleItems) {
            if (s.id == null)
                continue;
            putSession(String(s.id), s.startTime != null ? String(s.startTime) : null, s.endTime != null ? String(s.endTime) : null);
        }
        // 当日考勤里的课次：即使课表里已无该日/该 ID，也要能筛（导入补建课次后被物理删是常见情况）
        for (const row of allAttendancePage?.items ?? []) {
            if (normalizeBizDate(row.attendanceDate) !== dateStr)
                continue;
            if (row.scheduleId == null)
                continue;
            const id = String(row.scheduleId);
            if (byId.has(id))
                continue;
            const sch = scheduleList.find((x) => String(x.id) === id);
            if (sch) {
                putSession(id, sch.startTime != null ? String(sch.startTime) : null, sch.endTime != null ? String(sch.endTime) : null);
            }
            else {
                const times = sessionTimesFromAttendanceRemark(row.remark);
                putSession(id, times.start, times.end, `课次#${id}（考勤）`);
            }
        }
        if (sessionKey !== 'day' && !byId.has(sessionKey)) {
            const s = scheduleList.find((x) => String(x.id) === sessionKey);
            if (s) {
                putSession(sessionKey, s.startTime != null ? String(s.startTime) : null, s.endTime != null ? String(s.endTime) : null);
            }
            else {
                const att = (allAttendancePage?.items ?? []).find((r) => r.scheduleId != null && String(r.scheduleId) === sessionKey);
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
        queryFn: () => trainApi.getHomeworkMetrics(classIdNum),
        enabled: classIdNum != null && classIdNum > 0,
        retry: false,
    });
    const { data: mockMetrics, isLoading: mockMetricsLoading } = useQuery({
        queryKey: ['mock-metrics', classIdNum],
        queryFn: () => trainApi.getMockMetrics(classIdNum),
        enabled: classIdNum != null && classIdNum > 0,
        retry: false,
    });
    const { data: qualityMetrics, isLoading: qualityMetricsLoading } = useQuery({
        queryKey: ['follow-quality-metrics', classIdNum],
        queryFn: () => trainApi.getFollowQualityMetrics(classIdNum),
        enabled: classIdNum != null && classIdNum > 0,
        retry: false,
    });
    const { data: classHoursData, isLoading: classHoursLoading } = useQuery({
        queryKey: ['follow-class-hours', classIdNum],
        queryFn: () => trainApi.getFollowClassHours(classIdNum),
        enabled: classIdNum != null && classIdNum > 0,
        retry: false,
    });
    const { data: teacherDaysData, isLoading: teacherDaysLoading } = useQuery({
        queryKey: ['follow-teacher-days', classIdNum],
        queryFn: () => trainApi.getFollowTeacherDays(classIdNum),
        enabled: classIdNum != null && classIdNum > 0 && schedulePanel === 'teacherHours',
        retry: false,
    });
    useEffect(() => {
        if (teacherPaySavingRef.current)
            return;
        const next = {};
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
        mutationFn: (items) => trainApi.saveFollowTeacherPay(classIdNum, items),
        onSuccess: (data) => {
            teacherPaySavingRef.current = false;
            queryClient.setQueryData(['follow-teacher-days', classIdNum], data);
            message.success('课酬已保存', 1);
        },
        onError: (e) => {
            teacherPaySavingRef.current = false;
            message.error(e.message || '保存课酬失败');
        },
    });
    const buildTeacherPayItems = (draft) => (teacherDaysData?.teachers ?? []).map((row) => {
        const key = `${row.teacherName}|${row.teachForm ?? '未填'}`;
        return {
            teacherName: row.teacherName,
            teachForm: row.teachForm ?? '未填',
            ratePerDay: (draft[key] ?? {}).ratePerDay ?? null,
        };
    });
    const flushSaveTeacherPay = (draft) => {
        if (teacherPaySaveTimerRef.current) {
            clearTimeout(teacherPaySaveTimerRef.current);
            teacherPaySaveTimerRef.current = null;
        }
        if (classIdNum == null || classIdNum <= 0)
            return;
        const items = buildTeacherPayItems(draft ?? teacherPayDraftRef.current);
        if (items.length === 0)
            return;
        teacherPaySavingRef.current = true;
        saveTeacherPayMu.mutate(items);
    };
    const scheduleSaveTeacherPay = (draft) => {
        teacherPayDraftRef.current = draft;
        if (teacherPaySaveTimerRef.current) {
            clearTimeout(teacherPaySaveTimerRef.current);
        }
        teacherPaySaveTimerRef.current = setTimeout(() => {
            teacherPaySaveTimerRef.current = null;
            flushSaveTeacherPay(draft);
        }, 600);
    };
    useEffect(() => () => {
        if (teacherPaySaveTimerRef.current) {
            clearTimeout(teacherPaySaveTimerRef.current);
        }
    }, []);
    const exportTeacherDaysMu = useMutation({
        mutationFn: () => trainApi.exportFollowTeacherDays(classIdNum),
        onSuccess: () => message.success('已开始下载'),
        onError: (e) => message.error(e.message || '导出失败'),
    });
    const hoursByPeopleId = useMemo(() => {
        const map = new Map();
        for (const row of classHoursData?.students ?? []) {
            if (row.peopleId != null)
                map.set(String(row.peopleId), row);
        }
        return map;
    }, [classHoursData?.students]);
    /** 当日考勤行：合并「按日查询」与全量缓存，避免只拿到一批课次 */
    const dayAttendanceRows = useMemo(() => {
        if (!hasBizDate)
            return [];
        const byId = new Map();
        for (const row of allAttendancePage?.items ?? []) {
            if (normalizeBizDate(row.attendanceDate) !== dateStr)
                continue;
            if (row.id != null)
                byId.set(row.id, row);
        }
        for (const row of attendancePage?.items ?? []) {
            if (normalizeBizDate(row.attendanceDate) !== dateStr)
                continue;
            if (row.id != null)
                byId.set(row.id, row);
        }
        return [...byId.values()];
    }, [hasBizDate, dateStr, allAttendancePage?.items, attendancePage?.items]);
    const attendanceMap = useMemo(() => {
        const map = new Map();
        if (isSessionAttendance) {
            for (const row of attendancePage?.items ?? []) {
                if (row.userId != null)
                    map.set(String(row.userId), row);
            }
            return map;
        }
        // 按日：合并当日所有课次；有课次记录时不再被「按日空 schedule」行盖住
        const byUser = new Map();
        for (const row of dayAttendanceRows) {
            if (row.userId == null)
                continue;
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
        const map = new Map();
        const sourceRows = isSessionAttendance
            ? (attendancePage?.items ?? [])
            : dayAttendanceRows;
        const byUser = new Map();
        for (const row of sourceRows) {
            if (row.userId == null)
                continue;
            const key = String(row.userId);
            const list = byUser.get(key) ?? [];
            list.push(row);
            byUser.set(key, list);
        }
        for (const [uid, list] of byUser) {
            const seen = new Map();
            for (const row of list) {
                const info = resolveSessionPeriodLabel(row, scheduleList);
                if (!info)
                    continue;
                const key = `${info.period}|${info.timeLabel}`;
                if (!seen.has(key))
                    seen.set(key, info);
            }
            const labels = [...seen.values()]
                .sort((a, b) => {
                const pc = PERIOD_SORT[a.period] - PERIOD_SORT[b.period];
                return pc !== 0 ? pc : a.sortKey.localeCompare(b.sortKey);
            })
                .map((x) => (x.timeLabel ? `${x.period} ${x.timeLabel}` : x.period));
            if (labels.length)
                map.set(uid, labels);
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
            if (!att || att.status == null)
                continue;
            registered += 1;
            if (att.status === 1)
                onTime += 1;
            else if (att.status === 2)
                late += 1;
            else if (att.status === 3)
                early += 1;
            else if (att.status === 0)
                absent += 1;
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
        const schedulePeriod = new Map();
        const periodsByDate = new Map();
        for (const s of scheduleList) {
            const date = String(s.scheduleDate ?? '').slice(0, 10);
            const period = periodFromTime(s.startTime != null ? String(s.startTime) : null);
            if (!date || !period)
                continue;
            if (s.id != null) {
                schedulePeriod.set(String(s.id), { date, period });
            }
            const set = periodsByDate.get(date) ?? new Set();
            set.add(period);
            periodsByDate.set(date, set);
        }
        const bySlot = new Map();
        const byUser = new Map();
        /** 先扫一遍：哪些「日」已有课次考勤（有 scheduleId） */
        const datesWithSessionAttendance = new Set();
        for (const row of rows) {
            const date = String(row.attendanceDate ?? '').slice(0, 10);
            if (date && row.scheduleId != null)
                datesWithSessionAttendance.add(date);
        }
        const resolvePeriod = (row) => {
            const date = String(row.attendanceDate ?? '').slice(0, 10);
            if (!date)
                return null;
            // 1) 绑定课次：课表开始时间
            if (row.scheduleId != null) {
                const meta = schedulePeriod.get(String(row.scheduleId));
                if (meta?.period)
                    return meta.period;
            }
            // 2) 导入备注里的课次开始时间「导入课次：08:30-11:30」
            const fromRemark = periodFromTime(startTimeFromAttendanceRemark(row.remark));
            if (fromRemark)
                return fromRemark;
            // 3) 签到时间（完整日期时间须解析其中的时分，不能误读年份）
            const fromCheckIn = periodFromTime(row.checkInTime);
            if (fromCheckIn)
                return fromCheckIn;
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
            if (!date)
                continue;
            const period = resolvePeriod(row);
            if (!period)
                continue;
            const slotKey = `${date}|${period}`;
            const slot = bySlot.get(slotKey)
                ?? { date, period, presentUsers: new Set(), absentUsers: new Set() };
            const uid = row.userId != null ? String(row.userId) : '';
            const status = row.status;
            if (uid) {
                if (status != null && status > 0) {
                    slot.presentUsers.add(uid);
                    slot.absentUsers.delete(uid);
                }
                else if (status === 0 && !slot.presentUsers.has(uid)) {
                    slot.absentUsers.add(uid);
                }
            }
            bySlot.set(slotKey, slot);
            if (!uid)
                continue;
            const u = byUser.get(uid) ?? {
                name: row.userName || nameById.get(uid) || uid,
                present: 0,
                absent: 0,
                late: 0,
                early: 0,
                total: 0,
            };
            u.total += 1;
            if (status === 0)
                u.absent += 1;
            else if (status === 1)
                u.present += 1;
            else if (status === 2) {
                u.late += 1;
                u.present += 1;
            }
            else if (status === 3) {
                u.early += 1;
                u.present += 1;
            }
            if (row.userName)
                u.name = row.userName;
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
            if (dc !== 0)
                return dc;
            return PERIOD_SORT[a.period] - PERIOD_SORT[b.period];
        })
            .slice(0, 28);
        let sumPresent = 0;
        let sumExpected = 0;
        const dates = new Set();
        for (const row of dailyTrend) {
            sumPresent += row.present;
            sumExpected += row.total;
            dates.add(row.date);
        }
        // 累计出勤率：各时段「实到÷应到」合计（与表格口径一致）
        const cumulativeRate = sumExpected > 0 ? Math.round((sumPresent / sumExpected) * 1000) / 10 : 0;
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
        mutationFn: (student) => {
            if (!hasBizDate)
                return Promise.reject(new Error('请先选择业务日期'));
            const uid = toNumId(student.id);
            if (uid == null)
                return Promise.reject(new Error('无效学员ID'));
            const existing = attendanceMap.get(String(student.id));
            // 按日视图若折叠自课次记录，不可直接 update 课次行冒充按日
            const canUpdateExisting = existing?.id &&
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
        onError: (e) => message.error(e.message || '考勤操作失败（请确认账号具备 train:attendance 权限）'),
    });
    const markAbsentMu = useMutation({
        mutationFn: (student) => {
            if (!hasBizDate)
                return Promise.reject(new Error('请先选择业务日期'));
            const uid = toNumId(student.id);
            if (uid == null)
                return Promise.reject(new Error('无效学员ID'));
            const existing = attendanceMap.get(String(student.id));
            const canUpdateExisting = existing?.id &&
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
        onError: (e) => message.error(e.message || '考勤操作失败'),
    });
    const importMu = useMutation({
        mutationFn: (file) => {
            if (classIdNum == null)
                return Promise.reject(new Error('无效班级ID'));
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
            }
            else {
                message.warning('未写入有效考勤，请查看失败明细');
            }
        },
        onError: (err) => {
            const msg = err && typeof err === 'object' && 'message' in err
                ? String(err.message)
                : '导入失败';
            message.error(msg || '导入失败（请确认文件格式与 train:attendance:create 权限）');
        },
    });
    const attendanceColumns = [
        {
            title: '姓名',
            dataIndex: 'realname',
            width: 120,
            ellipsis: true,
            render: (v, row) => {
                const name = (v || row?.realname || '').trim();
                if (name)
                    return name;
                if (row?.phone)
                    return row.phone;
                if (row?.id_card)
                    return row.id_card;
                return '—';
            },
        },
        { title: '手机', dataIndex: 'phone', width: 130, render: (v) => v || '—' },
        {
            title: '考勤状态',
            width: 168,
            render: (_, row) => {
                const att = attendanceMap.get(String(row.id));
                const periodLabels = attendancePeriodLabelsByUser.get(String(row.id)) ?? [];
                if ((!att || att.status == null) && periodLabels.length === 0) {
                    return _jsx(Tag, { children: "\u672A\u767B\u8BB0" });
                }
                const meta = att?.status != null
                    ? ATTENDANCE_STATUS[att.status] ?? { text: String(att.status), color: 'default' }
                    : null;
                return (_jsxs(Space, { direction: "vertical", size: 2, style: { lineHeight: 1.3 }, children: [meta ? _jsx(Tag, { color: meta.color, children: meta.text }) : _jsx(Tag, { children: "\u672A\u767B\u8BB0" }), periodLabels.map((label) => (_jsx(Typography.Text, { type: "secondary", style: { fontSize: 12 }, children: label }, label)))] }));
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
                if (!hours || hours.attendedHours == null)
                    return '—';
                return formatClassHours(hours.attendedHours);
            },
        },
        {
            title: '完成率',
            width: 90,
            render: (_, row) => {
                const hours = hoursByPeopleId.get(String(row.id));
                if (!hours || hours.completionRate == null)
                    return '—';
                return `${Number(hours.completionRate).toFixed(1)}%`;
            },
        },
        {
            title: '操作',
            width: 220,
            fixed: 'right',
            render: (_, row) => (_jsxs(Space, { size: "small", children: [_jsx(Button, { type: "link", size: "small", onClick: () => setDetailStudent(row), children: "\u660E\u7EC6" }), _jsx(Button, { type: "link", size: "small", loading: signInMu.isPending, onClick: () => signInMu.mutate(row), children: "\u51FA\u52E4" }), _jsx(Button, { type: "link", size: "small", danger: true, loading: markAbsentMu.isPending, onClick: () => markAbsentMu.mutate(row), children: "\u7F3A\u52E4" })] })),
        },
    ];
    const leaders = classRoles.filter((r) => r.roleCode === 'monitor');
    const staffList = classRoles.filter((r) => r.roleCode === 'follow_staff');
    const chargeList = classRoles.filter((r) => r.roleCode === 'head_teacher');
    const followLogCountQuery = useQuery({
        queryKey: ['follow-op-logs', classIdNum, 'overview-count'],
        queryFn: () => trainApi.listFollowOpLogs(classIdNum),
        enabled: classIdNum != null && classIdNum > 0,
    });
    return (_jsxs("div", { className: "follow-workbench", children: [_jsxs("div", { className: "follow-workbench-header", children: [_jsxs(Space, { wrap: true, children: [_jsx(Button, { icon: _jsx(ArrowLeftOutlined, {}), onClick: onBack, children: "\u8FD4\u56DE\u73ED\u7EA7\u5217\u8868" }), _jsx(Typography.Title, { level: 4, style: { margin: 0 }, children: classItem.title || '未命名班级' }), _jsx(Tag, { color: "processing", children: "\u8DDF\u73ED\u5DE5\u4F5C\u53F0" })] }), _jsxs(Typography.Text, { type: "secondary", children: [[classItem.company_name, classItem.work_type_title, classItem.train_title].filter(Boolean).join(' · ') || '—', (classItem.date_start || classItem.date_end) &&
                                ` ｜ ${classItem.date_start || '—'} ~ ${classItem.date_end || '—'}`] })] }), _jsx(Card, { size: "small", style: { marginBottom: 12 }, children: _jsxs(Space, { wrap: true, size: "middle", children: [_jsxs("span", { children: ["\u4E1A\u52A1\u65E5\u671F\uFF1A", _jsx(DatePicker, { value: bizDate, allowClear: true, placeholder: "\u9009\u62E9\u6216\u6E05\u7A7A", onChange: (d) => {
                                        setBizDate(d);
                                        setSessionKey('day');
                                    } })] }), _jsxs("span", { children: ["\u8BFE\u6B21\uFF1A", _jsx(Select, { style: { width: 280 }, value: sessionKey, onChange: setSessionKey, disabled: !hasBizDate, options: sessionSelectOptions })] }), _jsx(Tag, { children: hasBizDate
                                ? isSessionAttendance
                                    ? '课次考勤'
                                    : scheduleItems.length
                                        ? `按日汇总 · 当日 ${scheduleItems.length} 课次`
                                        : sessionSelectOptions.length > 1
                                            ? `按日汇总 · 考勤课次 ${sessionSelectOptions.length - 1} 个`
                                            : '按日考勤 · 无排课'
                                : '未选业务日' })] }) }), _jsx(Tabs, { activeKey: activeTab, onChange: setActiveTab, items: [
                    {
                        key: 'overview',
                        label: '概览',
                        children: (_jsxs("div", { children: [_jsxs(Row, { gutter: [12, 12], style: { marginBottom: 16 }, children: [_jsx(Col, { xs: 12, sm: 8, md: 4, children: _jsx(Card, { size: "small", children: _jsx(Statistic, { title: "\u5B66\u5458\u6570", value: rosterTotal, prefix: _jsx(TeamOutlined, {}) }) }) }), _jsx(Col, { xs: 12, sm: 8, md: 4, children: _jsx(Card, { size: "small", children: _jsx(Statistic, { title: isSessionAttendance ? '本课次实到' : '当日实到', value: presentCount, suffix: `/ ${dayStats.base || '—'}` }) }) }), _jsx(Col, { xs: 12, sm: 8, md: 4, children: _jsxs(Card, { size: "small", children: [_jsx(Statistic, { title: isSessionAttendance ? '本课次出勤率' : '当日出勤率', value: attendanceRate, suffix: "%" }), _jsx(Progress, { percent: attendanceRate, showInfo: false, size: "small", status: attendanceRate < 80 ? 'exception' : 'normal' })] }) }), _jsx(Col, { xs: 12, sm: 8, md: 4, children: _jsxs(Card, { size: "small", loading: classStats.loading, children: [_jsx(Statistic, { title: "\u7D2F\u8BA1\u51FA\u52E4\u7387", value: classStats.cumulativeRate, suffix: "%", prefix: _jsx(CheckCircleOutlined, {}) }), _jsxs(Typography.Text, { type: "secondary", style: { fontSize: 12 }, children: [classStats.recordDays, " \u4E2A\u6709\u8003\u52E4\u65E5 \u00B7 ", classStats.recordCount, " \u6761\u8BB0\u5F55"] })] }) }), _jsx(Col, { xs: 12, sm: 8, md: 4, children: _jsx(Card, { size: "small", children: _jsx(Statistic, { title: "\u5F53\u65E5\u672A\u767B\u8BB0", value: dayStats.unregistered }) }) }), _jsx(Col, { xs: 12, sm: 8, md: 4, children: _jsx(Card, { size: "small", hoverable: true, onClick: () => setActiveTab('logs'), style: { cursor: 'pointer' }, children: _jsx(Statistic, { title: "\u8DDF\u73ED\u65E5\u5FD7", value: followLogCountQuery.data?.length ?? 0, prefix: _jsx(HistoryOutlined, {}), loading: followLogCountQuery.isLoading }) }) })] }), _jsx(Card, { size: "small", title: `${isSessionAttendance ? '本课次' : '当日'}考勤构成（${hasBizDate ? dateStr : '未选日期'}）`, style: { marginBottom: 16 }, children: _jsxs(Row, { gutter: [12, 12], children: [_jsx(Col, { xs: 12, sm: 8, md: 4, children: _jsx(Statistic, { title: "\u51FA\u52E4", value: dayStats.onTime, valueStyle: { color: '#389e0d' } }) }), _jsx(Col, { xs: 12, sm: 8, md: 4, children: _jsx(Statistic, { title: "\u8FDF\u5230", value: dayStats.late, valueStyle: { color: '#d48806' } }) }), _jsx(Col, { xs: 12, sm: 8, md: 4, children: _jsx(Statistic, { title: "\u65E9\u9000", value: dayStats.early, valueStyle: { color: '#d46b08' } }) }), _jsx(Col, { xs: 12, sm: 8, md: 4, children: _jsx(Statistic, { title: "\u7F3A\u52E4", value: dayStats.absent, valueStyle: { color: '#cf1322' } }) }), _jsx(Col, { xs: 12, sm: 8, md: 4, children: _jsx(Statistic, { title: "\u5DF2\u767B\u8BB0", value: dayStats.registered }) }), _jsxs(Col, { xs: 12, sm: 8, md: 4, children: [_jsx(Statistic, { title: "\u5F85\u767B\u8BB0", value: dayStats.unregistered }), _jsx(Button, { type: "link", style: { padding: 0 }, onClick: () => setActiveTab('attendance'), children: "\u53BB\u8003\u52E4 \u2192" })] })] }) }), _jsxs(Row, { gutter: [12, 12], style: { marginBottom: 16 }, children: [_jsx(Col, { xs: 24, lg: 12, children: _jsx(Card, { size: "small", className: "follow-overview-scroll-card", title: "\u8FD1\u65E5\u51FA\u52E4\u7387", loading: classStats.loading, children: classStats.dailyTrend.length === 0 ? (_jsx(Empty, { image: Empty.PRESENTED_IMAGE_SIMPLE, description: "\u6682\u65E0\u8003\u52E4\u8BB0\u5F55\uFF0C\u53EF\u5BFC\u5165\u8003\u52E4\u8868\u6216\u5F53\u65E5\u767B\u8BB0" })) : (_jsx(Table, { size: "small", rowKey: (r) => `${r.date}|${r.period}`, pagination: false, sticky: true, dataSource: classStats.dailyTrend, columns: [
                                                        { title: '日期', dataIndex: 'date', width: 110 },
                                                        {
                                                            title: '时段',
                                                            dataIndex: 'period',
                                                            width: 64,
                                                            render: (v) => _jsx(Tag, { children: v }),
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
                                                            render: (v) => (_jsxs(Space, { children: [_jsx(Progress, { percent: v, size: "small", style: { width: 72, margin: 0 }, status: v < 80 ? 'exception' : 'normal' }), _jsxs("span", { children: [v, "%"] })] })),
                                                        },
                                                        {
                                                            title: '',
                                                            width: 64,
                                                            render: (_, r) => (_jsx(Button, { type: "link", size: "small", style: { padding: 0 }, onClick: () => {
                                                                    setPendingPeriodNav({ date: r.date, period: r.period });
                                                                    setBizDate(dayjs(r.date));
                                                                    setActiveTab('attendance');
                                                                }, children: "\u67E5\u770B" })),
                                                        },
                                                    ] })) }) }), _jsx(Col, { xs: 24, lg: 12, children: _jsx(Card, { size: "small", className: "follow-overview-scroll-card", title: "\u5B66\u5458\u9884\u8B66", extra: _jsx(Typography.Text, { type: "secondary", children: "\u7F3A\u52E4\u22652 \u6216\u4E2A\u4EBA\u51FA\u52E4\u7387<80%" }), loading: classStats.loading, children: classStats.warnings.length === 0 ? (_jsx(Empty, { image: Empty.PRESENTED_IMAGE_SIMPLE, description: "\u6682\u65E0\u9884\u8B66\u5B66\u5458" })) : (_jsx(Table, { size: "small", rowKey: "userId", pagination: false, sticky: true, dataSource: classStats.warnings, columns: [
                                                        { title: '姓名', dataIndex: 'name', width: 100 },
                                                        { title: '缺勤', dataIndex: 'absent', width: 60 },
                                                        { title: '迟到', dataIndex: 'late', width: 60 },
                                                        {
                                                            title: '个人出勤率',
                                                            dataIndex: 'rate',
                                                            render: (v) => (_jsxs(Tag, { color: v < 80 ? 'error' : 'warning', children: [v, "%"] })),
                                                        },
                                                        {
                                                            title: '',
                                                            width: 70,
                                                            render: (_, r) => {
                                                                const stu = allStudents.find((s) => String(s.id) === r.userId);
                                                                return (_jsx(Button, { type: "link", size: "small", style: { padding: 0 }, disabled: !stu, onClick: () => stu && setDetailStudent(stu), children: "\u660E\u7EC6" }));
                                                            },
                                                        },
                                                    ] })) }) })] }), _jsx(Card, { size: "small", title: "\u73ED\u7EA7\u8BFE\u7A0B\u8868", style: { marginBottom: 16 }, loading: scheduleLoading, extra: _jsx(Button, { type: "link", style: { padding: 0 }, onClick: () => setActiveTab('schedule'), children: "\u7BA1\u7406\u8BFE\u8868 \u2192" }), children: scheduleStats.sessionCount === 0 ? (_jsx(Empty, { image: Empty.PRESENTED_IMAGE_SIMPLE, description: "\u6682\u65E0\u8BFE\u6B21", children: _jsx(Button, { type: "primary", size: "small", onClick: () => setActiveTab('schedule'), children: "\u53BB\u914D\u7F6E\u8BFE\u7A0B\u8868" }) })) : (_jsxs(Space, { direction: "vertical", size: 12, style: { width: '100%' }, children: [_jsxs(Row, { gutter: [12, 12], children: [_jsx(Col, { xs: 12, sm: 8, md: 4, children: _jsx(Statistic, { title: "\u8BFE\u6B21", value: scheduleStats.sessionCount, suffix: "\u8282" }) }), _jsx(Col, { xs: 12, sm: 8, md: 4, children: _jsx(Statistic, { title: "\u8BA1\u5212\u5B66\u65F6", value: formatClassHours(classHoursData?.plannedHours ?? scheduleStats.totalHours), suffix: "\u8BFE\u65F6" }) }), _jsx(Col, { xs: 12, sm: 8, md: 4, children: _jsx(Statistic, { title: "\u77E5\u8BC6\u70B9", value: scheduleStats.subjectCount, suffix: "\u4E2A" }) }), _jsx(Col, { xs: 12, sm: 8, md: 4, children: _jsx(Statistic, { title: "\u4E0A\u8BFE\u5929\u6570", value: scheduleStats.dayCount, suffix: "\u5929" }) }), _jsx(Col, { xs: 12, sm: 8, md: 4, children: _jsx(Statistic, { title: "\u6388\u8BFE\u8001\u5E08", value: scheduleStats.teacherCount, suffix: "\u4EBA" }) }), _jsx(Col, { xs: 12, sm: 8, md: 4, children: _jsx(Statistic, { title: "\u4ECA\u65E5\u8BFE\u6B21", value: scheduleStats.todayCount, suffix: "\u8282" }) })] }), _jsxs(Space, { wrap: true, size: [8, 4], style: { width: '100%', justifyContent: 'space-between' }, children: [_jsxs(Space, { wrap: true, size: 8, children: [scheduleStats.theoryCount > 0 ? (_jsxs(Tag, { color: "blue", children: ["\u7406\u8BBA ", scheduleStats.theoryCount, " \u8282"] })) : null, scheduleStats.practiceCount > 0 ? (_jsxs(Tag, { color: "green", children: ["\u5B9E\u64CD ", scheduleStats.practiceCount, " \u8282"] })) : null, (classHoursData?.unconvertibleSessions ?? 0) > 0 ? (_jsxs(Typography.Text, { type: "secondary", style: { fontSize: 12 }, children: ["\u6709 ", classHoursData?.unconvertibleSessions, " \u8282\u65E0\u6CD5\u6298\u7B97\uFF08\u65E0\u65F6\u95F4\u4E5F\u65E0\u5B66\u65F6\uFF09\uFF0C\u4E0D\u8BA1\u5165\u8BA1\u5212/\u5DF2\u4E0A\u8BFE\u65F6"] })) : scheduleStats.hoursKnown < scheduleStats.sessionCount ? (_jsx(Typography.Text, { type: "secondary", style: { fontSize: 12 }, children: scheduleStats.hoursKnown === 0
                                                                    ? '部分课次未填学时；有起止时间的已按 45 分钟折算计划学时'
                                                                    : `有 ${scheduleStats.sessionCount - scheduleStats.hoursKnown} 节未填学时，有时间的已按 45 分钟折算` })) : (_jsx(Typography.Text, { type: "secondary", style: { fontSize: 12 }, children: "\u8BA1\u5212\u5B66\u65F6\u6309\u8BFE\u8868\u8D77\u6B62\u65F6\u95F4 \u00F7 45 \u5206\u949F\uFF1B\u65E0\u65F6\u95F4\u5219\u7528\u8BFE\u8868\u5B66\u65F6" })), scheduleDirty ? _jsx(Tag, { color: "warning", children: "\u672A\u4FDD\u5B58" }) : null, scheduleManualEdited ? _jsx(Tag, { children: "\u5DF2\u8C03\u8BFE" }) : null] }), _jsx(Button, { type: "primary", size: "small", onClick: () => setActiveTab('schedule'), children: "\u6253\u5F00\u8BFE\u7A0B\u8868" })] })] })) }), _jsxs(Row, { gutter: [12, 12], style: { marginBottom: 16 }, children: [_jsx(Col, { xs: 24, lg: 12, children: _jsx(Card, { size: "small", title: "\u4F5C\u4E1A\u5B8C\u6210\u60C5\u51B5", loading: homeworkMetricsLoading, extra: _jsx(Button, { type: "link", style: { padding: 0 }, onClick: () => setActiveTab('homework'), children: "\u4F5C\u4E1A\u660E\u7EC6 \u2192" }), children: _jsxs(Space, { size: "large", wrap: true, children: [_jsx(Statistic, { title: "\u5DF2\u53D1\u5E03", value: homeworkMetrics?.taskCount ?? 0 }), _jsx(Statistic, { title: "\u5B8C\u6210\u7387", value: homeworkMetrics?.completionRate ?? 0, suffix: "%" }), _jsx(Statistic, { title: "\u5E73\u5747\u6B63\u786E\u7387", value: homeworkMetrics?.avgAccuracyRate ?? '—', suffix: homeworkMetrics?.avgAccuracyRate != null ? '%' : undefined }), _jsx(Statistic, { title: "\u5DF2\u4EA4/\u5E94\u4EA4", value: `${homeworkMetrics?.gradedItems ?? 0}/${homeworkMetrics?.totalItems ?? 0}` })] }) }) }), _jsx(Col, { xs: 24, lg: 12, children: _jsx(Card, { size: "small", title: "\u6A21\u62DF\u5377\u6982\u89C8", loading: mockMetricsLoading, extra: _jsx(Button, { type: "link", style: { padding: 0 }, onClick: () => setActiveTab('mock'), children: "\u6A21\u62DF\u8003\u660E\u7EC6 \u2192" }), children: _jsxs(Space, { size: "large", wrap: true, children: [_jsx(Statistic, { title: "\u5DF2\u53D1\u5E03", value: mockMetrics?.taskCount ?? 0 }), _jsx(Statistic, { title: "\u5B8C\u6210\u7387", value: mockMetrics?.completionRate ?? 0, suffix: "%" }), _jsx(Statistic, { title: "\u5E73\u5747\u6B63\u786E\u7387", value: mockMetrics?.avgAccuracyRate ?? '—', suffix: mockMetrics?.avgAccuracyRate != null ? '%' : undefined }), _jsx(Statistic, { title: "\u5DF2\u4EA4/\u5E94\u4EA4", value: `${mockMetrics?.gradedItems ?? 0}/${mockMetrics?.totalItems ?? 0}` })] }) }) })] }), _jsxs(Row, { gutter: [12, 12], style: { marginBottom: 16 }, children: [_jsx(Col, { xs: 24, lg: 12, children: _jsx(Card, { size: "small", className: "follow-overview-scroll-card", title: "\u4F5C\u4E1A\u9884\u8B66", extra: _jsx(Typography.Text, { type: "secondary", children: "\u8FDE\u7EED\u672A\u4EA4\u22652 \u6216\u5E73\u5747\u6B63\u786E\u7387<60%" }), loading: homeworkMetricsLoading, children: (homeworkMetrics?.warnings?.length ?? 0) === 0 ? (_jsx(Empty, { image: Empty.PRESENTED_IMAGE_SIMPLE, description: "\u6682\u65E0\u4F5C\u4E1A\u9884\u8B66" })) : (_jsx(Table, { size: "small", rowKey: (r) => `${r.peopleId}-${r.type}`, pagination: false, sticky: true, dataSource: homeworkMetrics?.warnings ?? [], columns: [
                                                        { title: '姓名', dataIndex: 'userName', width: 100 },
                                                        {
                                                            title: '类型',
                                                            dataIndex: 'type',
                                                            width: 100,
                                                            render: (t) => t === 'miss' ? _jsx(Tag, { color: "warning", children: "\u672A\u4EA4" }) : _jsx(Tag, { color: "error", children: "\u6B63\u786E\u7387\u4F4E" }),
                                                        },
                                                        { title: '说明', dataIndex: 'message', ellipsis: true },
                                                    ] })) }) }), _jsx(Col, { xs: 24, lg: 12, children: _jsx(Card, { size: "small", className: "follow-overview-scroll-card", title: "\u6A21\u62DF\u5377\u9884\u8B66", extra: _jsx(Typography.Text, { type: "secondary", children: "\u8FDE\u7EED\u672A\u4EA4\u22652 \u6216\u5E73\u5747\u6B63\u786E\u7387<60%" }), loading: mockMetricsLoading, children: (mockMetrics?.warnings?.length ?? 0) === 0 ? (_jsx(Empty, { image: Empty.PRESENTED_IMAGE_SIMPLE, description: "\u6682\u65E0\u6A21\u62DF\u5377\u9884\u8B66" })) : (_jsx(Table, { size: "small", rowKey: (r) => `mock-${r.peopleId}-${r.type}`, pagination: false, sticky: true, dataSource: mockMetrics?.warnings ?? [], columns: [
                                                        { title: '姓名', dataIndex: 'userName', width: 100 },
                                                        {
                                                            title: '类型',
                                                            dataIndex: 'type',
                                                            width: 100,
                                                            render: (t) => t === 'miss' ? _jsx(Tag, { color: "warning", children: "\u672A\u4EA4" }) : _jsx(Tag, { color: "error", children: "\u6B63\u786E\u7387\u4F4E" }),
                                                        },
                                                        { title: '说明', dataIndex: 'message', ellipsis: true },
                                                    ] })) }) })] }), _jsx(Card, { size: "small", title: "\u73ED\u7EA7\u5237\u9898\u8BD5\u9898", style: { marginBottom: 16 }, loading: practiceScopeLoading, extra: _jsx(Button, { type: "link", style: { padding: 0 }, onClick: () => setActiveTab('practice'), children: practiceScope?.configured ? '查看学情 / 调整配置 →' : '去配置 →' }), children: !practiceScope?.configured || !(practiceScope.courses?.length > 0) ? (_jsx(Empty, { image: Empty.PRESENTED_IMAGE_SIMPLE, description: "\u5C1A\u672A\u914D\u7F6E\u672C\u73ED\u53EF\u5237\u79D1\u76EE\uFF1B\u5B66\u5458\u4ECE App \u73ED\u7EA7\u5165\u53E3\u5237\u9898\u524D\u9700\u5148\u914D\u7F6E", children: _jsx(Button, { type: "primary", size: "small", onClick: () => setActiveTab('practice'), children: "\u53BB\u5237\u9898 Tab \u914D\u7F6E" }) })) : (_jsxs(Space, { direction: "vertical", size: 8, style: { width: '100%' }, children: [_jsxs("div", { children: [_jsx(Typography.Text, { type: "secondary", style: { marginRight: 8 }, children: "\u79D1\u76EE" }), _jsx(Space, { wrap: true, size: [4, 4], children: practiceScope.courses.map((c) => (_jsx(Tag, { color: "blue", children: c.title || `科目#${c.courseId}` }, c.courseId))) }), practiceScope.enabled === 0 && (_jsx(Tag, { color: "default", style: { marginLeft: 8 }, children: "\u5DF2\u505C\u7528" }))] }), _jsxs(Typography.Text, { type: "secondary", children: ["\u77E5\u8BC6\u70B9\uFF1A", practiceScope.knowledgeIds?.length
                                                        ? `已限定 ${practiceScope.knowledgeIds.length} 个`
                                                        : '所选科目下不限知识点', ' · ', "\u5F00\u653E\u65E5\u671F\uFF1A", practiceScope.practiceStartTime || practiceScope.practiceEndTime
                                                        ? `${practiceScope.practiceStartTime || '不限'} ~ ${practiceScope.practiceEndTime || '不过期'}`
                                                        : '不限（不过期）'] }), practiceScope.remark ? (_jsxs(Typography.Text, { type: "secondary", children: ["\u5907\u6CE8\uFF1A", practiceScope.remark] })) : null] })) }), _jsxs(Row, { gutter: [12, 12], children: [_jsx(Col, { xs: 24, md: 12, children: _jsx(Card, { size: "small", title: "\u89D2\u8272\u6458\u8981", children: _jsxs(Space, { direction: "vertical", style: { width: '100%' }, children: [_jsxs("div", { children: ["\u73ED\u957F\uFF1A", leaders.map((s) => s.displayName).join('、') || '未设置'] }), _jsxs("div", { children: ["\u6559\u52A1\u4EBA\u5458\uFF1A", [...staffList, ...chargeList].map((s) => s.displayName).join('、') || '未设置'] }), _jsxs("div", { children: ["\u7406\u8BBA\u8001\u5E08\uFF1A", classRoles.filter((r) => r.roleCode === 'lead_teacher').map((s) => s.displayName).join('、') || '未设置'] }), _jsxs("div", { children: ["\u5B9E\u64CD\u8001\u5E08\uFF1A", classRoles.filter((r) => r.roleCode === 'assistant').map((s) => s.displayName).join('、') || '未设置'] }), _jsx(Typography.Text, { type: "secondary", children: "\u89D2\u8272\u5728\u5F00\u73ED\u51C6\u5907\u300C\u73ED\u7EA7\u89D2\u8272\u300D\u4E0E\u672C\u9875\u540C\u6B65\u7EF4\u62A4\u3002" })] }) }) }), _jsx(Col, { xs: 24, md: 12, children: _jsx(Card, { size: "small", title: "\u4ECA\u65E5\u5F85\u529E", children: _jsxs(Space, { direction: "vertical", children: [_jsxs("div", { children: ["\u5F85\u767B\u8BB0\u8003\u52E4\uFF1A", dayStats.unregistered, " \u4EBA"] }), _jsxs("div", { children: ["\u9884\u8B66\u5B66\u5458\uFF1A", classStats.warnings.length, " \u4EBA"] }), _jsx("div", { children: "\u8FC7\u7A0B\u7559\u75D5\u3001\u4F5C\u4E1A\u6279\u6539\uFF1A\u80FD\u529B\u5EFA\u8BBE\u4E2D" }), _jsx(Button, { type: "link", style: { padding: 0 }, onClick: () => setActiveTab('attendance'), children: "\u53BB\u8003\u52E4 \u2192" }), _jsx(Button, { type: "link", style: { padding: 0 }, onClick: () => setActiveTab('schedule'), children: "\u53BB\u8BFE\u7A0B\u8868 \u2192" }), _jsx(Button, { type: "link", style: { padding: 0 }, onClick: () => setActiveTab('roles'), children: "\u53BB\u89D2\u8272 \u2192" })] }) }) })] })] })),
                    },
                    {
                        key: 'attendance',
                        label: '考勤',
                        children: (_jsxs("div", { children: [attendanceError && (_jsx(Alert, { type: "warning", showIcon: true, style: { marginBottom: 12 }, message: "\u8003\u52E4\u63A5\u53E3\u6682\u4E0D\u53EF\u7528\u6216\u65E0\u6743\u9650", description: "\u8BF7\u786E\u8BA4\u89D2\u8272\u5177\u5907 train:attendance:view / create\u3002\u4ECD\u53EF\u67E5\u770B\u5B66\u5458\u540D\u5355\u3002" })), _jsxs(Space, { wrap: true, style: { marginBottom: 12 }, children: [_jsx(Upload, { accept: ".xls,.xlsx,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", showUploadList: false, beforeUpload: (file) => {
                                                const name = file.name.toLowerCase();
                                                if (!name.endsWith('.xls') && !name.endsWith('.xlsx')) {
                                                    message.error('请上传 .xls 或 .xlsx 考勤文件');
                                                    return Upload.LIST_IGNORE;
                                                }
                                                importMu.mutate(file);
                                                return false;
                                            }, children: _jsx(Button, { type: "primary", icon: _jsx(UploadOutlined, {}), loading: importMu.isPending, disabled: classIdNum == null, children: "\u5BFC\u5165\u8003\u52E4\u8868" }) }), _jsx(Typography.Text, { type: "secondary", children: "\u652F\u6301\u4EBA\u793E/\u57F9\u8BAD\u5E73\u53F0\u300C\u8003\u52E4\u4FE1\u606F\u5217\u8868\u300D.xls/.xlsx\uFF1B\u6309\u8EAB\u4EFD\u8BC1\u5339\u914D\u672C\u73ED\u5B66\u5458\uFF0C\u6709\u4E0A\u8BFE/\u4E0B\u8BFE\u65F6\u95F4\u65F6\u6309\u8BFE\u6B21\u884C\u5199\u5165\uFF08\u5339\u914D\u8BFE\u8868\uFF0C\u5FC5\u8981\u65F6\u81EA\u52A8\u8865\u5EFA\u8BFE\u6B21\uFF09\uFF0C\u5E76\u6309\u300C\u662F\u5426\u65F7\u8BFE\u300D\u8BA1\u8BFE\u65F6\u3002\u65E0\u65F6\u6BB5\u65F6\u4ECD\u5408\u5E76\u4E3A\u6309\u65E5\u8BB0\u5F55\u3002" })] }), _jsx(Table, { rowKey: (r) => String(r.id), loading: studentsLoading || attendanceLoading || importMu.isPending, columns: attendanceColumns, dataSource: students, pagination: {
                                        current: studentPage,
                                        pageSize: 50,
                                        total: studentTotal,
                                        onChange: setStudentPage,
                                        showTotal: (t) => `共 ${t} 人`,
                                    }, scroll: { x: 920 }, locale: { emptyText: '暂无学员，请先在开班准备维护名单' } })] })),
                    },
                    {
                        key: 'schedule',
                        label: '课程表',
                        children: (_jsx("div", { children: _jsx(Card, { size: "small", title: _jsxs(Space, { size: "middle", wrap: true, children: [_jsx(Button, { type: "link", style: {
                                                padding: 0,
                                                height: 'auto',
                                                fontWeight: schedulePanel === 'list' ? 600 : 400,
                                                color: schedulePanel === 'list' ? undefined : 'rgba(0,0,0,0.45)',
                                            }, onClick: () => setSchedulePanel('list'), children: "\u73ED\u7EA7\u8BFE\u7A0B\u8868" }), _jsx(Button, { type: "link", style: {
                                                padding: 0,
                                                height: 'auto',
                                                fontWeight: schedulePanel === 'teacherHours' ? 600 : 400,
                                                color: schedulePanel === 'teacherHours' ? undefined : 'rgba(0,0,0,0.65)',
                                            }, onClick: () => setSchedulePanel('teacherHours'), children: "\u6559\u5E08\u4E0A\u8BFE\u65F6\u957F" })] }), style: { marginBottom: 16 }, loading: schedulePanel === 'list'
                                    ? scheduleLoading || importKcbiaoMu.isPending || saveScheduleMu.isPending
                                    : false, extra: schedulePanel === 'teacherHours' ? (_jsxs(Space, { wrap: true, size: "small", children: [_jsx(Button, { size: "small", icon: _jsx(ExportOutlined, {}), loading: exportTeacherDaysMu.isPending, disabled: (teacherDaysData?.teachers?.length ?? 0) === 0, onClick: () => exportTeacherDaysMu.mutate(), children: "\u5BFC\u51FA\u8BFE\u916C\u8868" }), saveTeacherPayMu.isPending ? (_jsx(Typography.Text, { type: "secondary", children: "\u4FDD\u5B58\u4E2D\u2026" })) : null, _jsx(Button, { size: "small", onClick: () => setSchedulePanel('list'), children: "\u8FD4\u56DE\u8BFE\u8868" })] })) : (_jsxs(Space, { wrap: true, size: "small", children: [_jsxs(Typography.Text, { type: "secondary", children: ["\u5171 ", scheduleList.length, " \u8282", scheduleItems.length > 0 ? ` · 业务日 ${scheduleItems.length} 节` : '', scheduleDirty ? ' · 未保存' : '', scheduleManualEdited ? ' · 已调课' : ''] }), _jsx(Button, { size: "small", icon: _jsx(PlusOutlined, {}), onClick: () => openScheduleEditor(), children: "\u6DFB\u52A0" }), hasKcbiaoFile ? (_jsx(Popconfirm, { title: "\u7528\u5F00\u73ED\u4E0A\u4F20\u7684\u8BFE\u8868\u8986\u76D6\u5F53\u524D\u7ED3\u6784\u5316\u8BFE\u6B21\uFF1F", description: scheduleManualEdited
                                                ? '本班已在跟班调过课，将弹出二次确认；开班原 Excel 不会被改写。'
                                                : '跟班已改过的课次将被替换；开班原 Excel 文件不会被改写。', onConfirm: () => runImportKcbiao(true), disabled: !hasKcbiaoFile, children: _jsx(Button, { size: "small", icon: _jsx(ReloadOutlined, {}), loading: importKcbiaoMu.isPending, children: "\u91CD\u5BFC\u5F00\u73ED\u8BFE\u8868" }) })) : null, _jsx(Button, { size: "small", icon: _jsx(ExportOutlined, {}), disabled: scheduleList.length === 0, loading: exportScheduleMu.isPending, onClick: runExportSchedule, children: "\u5BFC\u51FA" }), _jsx(Button, { size: "small", type: "primary", disabled: !scheduleDirty, loading: saveScheduleMu.isPending, onClick: () => saveScheduleMu.mutate(undefined), children: "\u4FDD\u5B58\u8BFE\u8868" })] })), children: schedulePanel === 'teacherHours' ? (_jsxs(Space, { direction: "vertical", size: 16, style: { width: '100%' }, children: [_jsx(Alert, { type: "info", showIcon: true, message: "\u5929\u6570\u6309\u672C\u73ED\u8BFE\u8868\u73B0\u7B97\uFF081 \u8BFE\u6B21 = 0.5 \u5929\uFF0C\u540C\u65E5\u6700\u591A 2 \u8282\uFF09\uFF1B\u8BFE\u916C\u4E3A\u672C\u73ED\u5355\u4EF7\uFF0C\u4FEE\u6539\u540E\u81EA\u52A8\u4FDD\u5B58\uFF1B\u5408\u8BA1 = \u5DF2\u4E0A\u5929\u6570 \u00D7 \u5355\u4EF7\uFF08\u56DB\u820D\u4E94\u5165\u5230\u5143\uFF09\u3002\u6309\u8001\u5E08\u00D7\u6388\u8BFE\u5F62\u5F0F\u5206\u884C\u3002" }), _jsxs(Row, { gutter: [12, 12], children: [_jsx(Col, { xs: 12, sm: 8, md: 4, children: _jsx(Statistic, { title: "\u5DF2\u4E0A\u5929\u6570\u5408\u8BA1", value: teacherDaysData?.taughtDaysSum ?? '—', suffix: "\u5929", loading: teacherDaysLoading }) }), _jsx(Col, { xs: 12, sm: 8, md: 4, children: _jsx(Statistic, { title: "\u8BA1\u5212\u5929\u6570\u5408\u8BA1", value: teacherDaysData?.plannedDaysSum ?? '—', suffix: "\u5929", loading: teacherDaysLoading }) }), _jsx(Col, { xs: 12, sm: 8, md: 4, children: _jsx(Statistic, { title: "\u8BFE\u916C\u5408\u8BA1", value: teacherPayDisplaySum ?? '—', suffix: "\u5143", loading: teacherDaysLoading }) }), _jsx(Col, { xs: 12, sm: 8, md: 4, children: _jsx(Statistic, { title: "\u672A\u6307\u6D3E\u8001\u5E08\u8BFE\u6B21", value: teacherDaysData?.unassignedSessionCount ?? 0, loading: teacherDaysLoading }) }), _jsx(Col, { xs: 12, sm: 8, md: 4, children: _jsx(Statistic, { title: "\u8D85\u9650\u8BFE\u6B21", value: teacherDaysData?.overflowSessionCount ?? 0, valueStyle: (teacherDaysData?.overflowSessionCount ?? 0) > 0
                                                            ? { color: '#d48806' }
                                                            : undefined, loading: teacherDaysLoading }) })] }), (teacherDaysData?.teachers?.length ?? 0) === 0 && !teacherDaysLoading ? (_jsx(Empty, { image: Empty.PRESENTED_IMAGE_SIMPLE, description: "\u672C\u73ED\u8BFE\u6B21\u5C1A\u672A\u6307\u6D3E\u8001\u5E08\uFF0C\u8BF7\u56DE\u8BFE\u8868\u8865\u5145\u300C\u8001\u5E08\u300D", children: _jsx(Button, { type: "primary", onClick: () => setSchedulePanel('list'), children: "\u8FD4\u56DE\u8BFE\u8868" }) })) : (_jsx(Table, { size: "small", rowKey: (r) => `${r.teacherName}|${r.teachForm ?? '未填'}`, loading: teacherDaysLoading, pagination: false, dataSource: teacherDaysData?.teachers ?? [], columns: [
                                                { title: '授课老师', dataIndex: 'teacherName', ellipsis: true, width: 120 },
                                                {
                                                    title: '授课形式',
                                                    dataIndex: 'teachForm',
                                                    width: 88,
                                                    render: (v) => v || '未填',
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
                                                    render: (v) => v == null ? '—' : Number(v).toFixed(1),
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
                                                    render: (v) => v == null ? '—' : Number(v).toFixed(1),
                                                },
                                                {
                                                    title: '超限',
                                                    dataIndex: 'overflowSessionCount',
                                                    width: 72,
                                                    align: 'right',
                                                    render: (v) => (v ?? 0) > 0 ? _jsx(Tag, { color: "warning", children: v }) : '—',
                                                },
                                                {
                                                    title: '课酬（元/天）',
                                                    dataIndex: 'ratePerDay',
                                                    width: 140,
                                                    render: (_, row) => {
                                                        const key = `${row.teacherName}|${row.teachForm ?? '未填'}`;
                                                        const draft = teacherPayDraft[key];
                                                        return (_jsx(InputNumber, { size: "small", min: 0, precision: 2, style: { width: '100%' }, value: draft?.ratePerDay ?? undefined, placeholder: "\u672A\u8BBE", onChange: (val) => {
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
                                                            }, onBlur: () => {
                                                                if (teacherPaySaveTimerRef.current) {
                                                                    flushSaveTeacherPay();
                                                                }
                                                            } }));
                                                    },
                                                },
                                                {
                                                    title: '合计课酬',
                                                    key: 'payTotal',
                                                    width: 100,
                                                    align: 'right',
                                                    render: (_, row) => {
                                                        const key = `${row.teacherName}|${row.teachForm ?? '未填'}`;
                                                        const draft = teacherPayDraft[key];
                                                        const rate = draft?.ratePerDay ?? row.ratePerDay;
                                                        const days = row.taughtDays;
                                                        if (rate == null || days == null)
                                                            return '—';
                                                        return Math.round(Number(rate) * Number(days));
                                                    },
                                                },
                                            ] }))] })) : (_jsxs(_Fragment, { children: [scheduleManualEdited ? (_jsx(Alert, { type: "warning", showIcon: true, style: { marginBottom: 8 }, message: "\u5F53\u524D\u8BFE\u8868\u5DF2\u5728\u8DDF\u73ED\u624B\u5DE5\u8C03\u6574\uFF0C\u4EE5\u7ED3\u6784\u5316\u8BFE\u6B21\u4E3A\u51C6\uFF1B\u91CD\u5BFC\u5F00\u73ED Excel \u9700\u4E8C\u6B21\u786E\u8BA4\u3002" })) : null, scheduleList.length === 0 ? (_jsx(Empty, { image: Empty.PRESENTED_IMAGE_SIMPLE, description: hasKcbiaoFile
                                                ? '尚未导入结构化课次；可从开班上传的课表导入，或直接添加'
                                                : '暂无课表：请先在开班准备上传课程表，或在此直接添加课次', children: _jsxs(Space, { children: [hasKcbiaoFile ? (_jsx(Button, { type: "primary", loading: importKcbiaoMu.isPending, onClick: () => runImportKcbiao(false), children: "\u4ECE\u5F00\u73ED\u8BFE\u8868\u5BFC\u5165" })) : null, _jsx(Button, { onClick: () => openScheduleEditor(), children: "\u6DFB\u52A0\u8BFE\u6B21" }), hasKcbiaoFile && (classItem.kcbiao_src || classItem.kcbiao_file) ? (_jsx(Button, { type: "link", onClick: () => {
                                                            const url = classItem.kcbiao_src || classItem.kcbiao_file;
                                                            if (url)
                                                                void downloadUrlFile(url, '开班课程表');
                                                        }, children: "\u4E0B\u8F7D\u539F\u6587\u4EF6" })) : null] }) })) : (_jsx(Table, { size: "small", rowKey: (r) => r._key, pagination: false, sticky: true, dataSource: scheduleList, tableLayout: "fixed", rowClassName: (r) => hasBizDate && String(r.scheduleDate ?? '') === dateStr
                                                ? 'follow-schedule-row-today'
                                                : '', columns: [
                                                {
                                                    title: '日期',
                                                    dataIndex: 'scheduleDate',
                                                    width: 70,
                                                    render: (v) => (_jsxs(Space, { size: 4, children: [_jsx("span", { children: v || '—' }), hasBizDate && v === dateStr ? _jsx(Tag, { color: "processing", children: "\u4ECA\u65E5" }) : null] })),
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
                                                    render: (_, r) => `${String(r.startTime || '—')}-${String(r.endTime || '—')}`,
                                                },
                                                {
                                                    title: '培训内容',
                                                    dataIndex: 'courseName',
                                                    width: 514,
                                                    ellipsis: true,
                                                    render: (v) => v || '—',
                                                },
                                                {
                                                    title: '学时',
                                                    dataIndex: 'classHours',
                                                    width: 30,
                                                    render: (v) => v || '—',
                                                },
                                                {
                                                    title: '授课形式',
                                                    dataIndex: 'teachForm',
                                                    width: 60,
                                                    ellipsis: true,
                                                    render: (v) => v || '—',
                                                },
                                                {
                                                    title: '老师',
                                                    dataIndex: 'teacherName',
                                                    width: 60,
                                                    ellipsis: true,
                                                    render: (v) => v || '—',
                                                },
                                                {
                                                    title: '操作',
                                                    width: 150,
                                                    render: (_, r) => (_jsxs(Space, { size: 0, wrap: false, children: [_jsx(Button, { type: "link", size: "small", icon: _jsx(EditOutlined, {}), onClick: () => openScheduleEditor(r), children: "\u76F4\u63A5\u6539\u8BFE" }), _jsx(Popconfirm, { title: "\u5220\u9664\u8BE5\u8BFE\u6B21\uFF1F", onConfirm: () => removeScheduleRow(r._key), children: _jsx(Button, { type: "link", size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), children: "\u5220\u9664" }) }), _jsx(Button, { type: "link", size: "small", disabled: !r.scheduleDate, onClick: () => {
                                                                    if (r.scheduleDate) {
                                                                        setBizDate(dayjs(String(r.scheduleDate)));
                                                                        setSessionKey(String(r.id ?? 'day'));
                                                                        setActiveTab('attendance');
                                                                    }
                                                                }, children: "\u8003\u52E4" })] })),
                                                },
                                            ] }))] })) }) })),
                    },
                    {
                        key: 'schedule-change',
                        label: pendingChangeCount > 0 ? `调课审批 (${pendingChangeCount})` : '调课审批',
                        children: classIdNum != null && classIdNum > 0 ? (_jsx(FollowScheduleChangeTab, { classId: classIdNum })) : (_jsx(Empty, { description: "\u65E0\u6548\u73ED\u7EA7" })),
                    },
                    {
                        key: 'homework',
                        label: '作业',
                        children: classIdNum != null && classIdNum > 0 ? (_jsx(FollowHomeworkTab, { classId: classIdNum })) : (_jsx(Empty, { description: "\u65E0\u6548\u73ED\u7EA7" })),
                    },
                    {
                        key: 'practice',
                        label: '刷题',
                        children: (_jsx(FollowPracticeTab, { classId: classId, classTitle: classItem.title, active: activeTab === 'practice' })),
                    },
                    {
                        key: 'review-sync',
                        label: '同步学',
                        children: (_jsx(FollowReviewSyncTab, { classId: classId, classTitle: classItem.title, active: activeTab === 'review-sync' })),
                    },
                    {
                        key: 'mock',
                        label: '模拟考',
                        children: classIdNum != null && classIdNum > 0 ? (_jsx(FollowMockExamTab, { classId: classIdNum })) : (_jsx(Empty, { description: "\u65E0\u6548\u73ED\u7EA7" })),
                    },
                    {
                        key: 'roles',
                        label: '角色',
                        children: classId != null ? (_jsx(ClassRolesPanel, { classId: classId, contextHint: "\u5728\u6B64\u6307\u6D3E\u540E\uFF0C\u5F00\u73ED\u51C6\u5907\u300C\u73ED\u7EA7\u89D2\u8272\u300D\u5C06\u540C\u6B65\u663E\u793A\u3002" })) : (_jsx(Empty, { description: "\u65E0\u6548\u73ED\u7EA7" })),
                    },
                    {
                        key: 'evidence',
                        label: '留痕',
                        children: classIdNum != null ? (_jsx(FollowEvidenceTab, { classId: classIdNum })) : (_jsx(Empty, { description: "\u65E0\u6548\u73ED\u7EA7" })),
                    },
                    {
                        key: 'logs',
                        label: '日志·督导',
                        children: classIdNum != null ? (_jsx(FollowLogsSupervisionTab, { classId: classIdNum, defaultBizDate: bizDate })) : (_jsx(Empty, { description: "\u65E0\u6548\u73ED\u7EA7" })),
                    },
                    {
                        key: 'metrics',
                        label: '指标',
                        children: (_jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: "middle", children: [_jsxs(Row, { gutter: [12, 12], children: [_jsx(Col, { xs: 12, md: 6, children: _jsx(Card, { size: "small", children: _jsx(Statistic, { title: "\u51FA\u52E4\u7387\uFF08\u5F53\u65E5\uFF09", value: attendanceRate, suffix: "%", prefix: _jsx(CheckCircleOutlined, {}) }) }) }), _jsx(Col, { xs: 12, md: 6, children: _jsx(Card, { size: "small", loading: classStats.loading, children: _jsx(Statistic, { title: "\u51FA\u52E4\u7387\uFF08\u7D2F\u8BA1\uFF09", value: classStats.cumulativeRate, suffix: "%", prefix: _jsx(LineChartOutlined, {}) }) }) }), _jsx(Col, { xs: 12, md: 6, children: _jsx(Card, { size: "small", loading: qualityMetricsLoading || homeworkMetricsLoading, children: _jsx(Statistic, { title: "\u4F5C\u4E1A\u5B8C\u6210\u7387", value: qualityMetrics?.homeworkCompletionRate ?? homeworkMetrics?.completionRate ?? '—', suffix: qualityMetrics?.homeworkCompletionRate != null || homeworkMetrics?.completionRate != null
                                                        ? '%'
                                                        : undefined, prefix: _jsx(EditOutlined, {}) }) }) }), _jsx(Col, { xs: 12, md: 6, children: _jsx(Card, { size: "small", loading: qualityMetricsLoading, children: _jsx(Statistic, { title: "\u6EE1\u610F\u5EA6", value: qualityMetrics?.satisfactionAvg ?? '—', suffix: qualityMetrics?.satisfactionCount
                                                        ? `（${qualityMetrics.satisfactionCount} 份）`
                                                        : undefined, prefix: _jsx(StarOutlined, {}) }) }) }), _jsx(Col, { xs: 12, md: 6, children: _jsx(Card, { size: "small", loading: qualityMetricsLoading, children: _jsx(Statistic, { title: "\u8BA1\u5212\u5B66\u65F6", value: formatClassHours(qualityMetrics?.plannedHours), suffix: "\u8BFE\u65F6" }) }) }), _jsx(Col, { xs: 12, md: 6, children: _jsx(Card, { size: "small", loading: qualityMetricsLoading, children: _jsx(Statistic, { title: "\u5DF2\u4E0A\u8BFE\u65F6\uFF08\u4EBA\u5747\uFF09", value: formatClassHours(qualityMetrics?.attendedHoursAvg), suffix: "\u8BFE\u65F6" }) }) }), _jsx(Col, { xs: 12, md: 6, children: _jsx(Card, { size: "small", loading: qualityMetricsLoading, children: _jsx(Statistic, { title: "\u8BFE\u65F6\u5B8C\u6210\u7387", value: qualityMetrics?.hoursCompletionAvg ?? '—', suffix: qualityMetrics?.hoursCompletionAvg != null ? '%' : undefined }) }) }), _jsx(Col, { xs: 12, md: 6, children: _jsx(Card, { size: "small", loading: qualityMetricsLoading || mockMetricsLoading, children: _jsx(Statistic, { title: "\u6A21\u62DF\u901A\u8FC7\u7387", value: qualityMetrics?.mockPassRate ?? '—', suffix: qualityMetrics?.mockPassRate != null ? '%' : undefined }) }) }), _jsx(Col, { xs: 12, md: 6, children: _jsx(Card, { size: "small", loading: qualityMetricsLoading, children: _jsx(Statistic, { title: "\u8003\u8BD5\u901A\u8FC7\u7387", value: qualityMetrics?.examPassRate ?? '—', suffix: qualityMetrics?.examPassRate != null ? '%' : undefined }) }) }), _jsx(Col, { xs: 12, md: 6, children: _jsxs(Card, { size: "small", loading: qualityMetricsLoading, children: [_jsx(Statistic, { title: "\u5F71\u50CF\u7559\u75D5", value: qualityMetrics?.evidencePhotoCount != null
                                                            ? Number(qualityMetrics.evidencePhotoCount)
                                                            : qualityMetrics?.evidenceReady
                                                                ? 1
                                                                : 0, suffix: "\u5F20" }), _jsx(Typography.Text, { type: "secondary", style: { fontSize: 12 }, children: qualityMetrics?.evidenceReady ? '可生成培训照片' : '请在留痕页手动添加' })] }) }), _jsx(Col, { xs: 12, md: 6, children: _jsxs(Card, { size: "small", loading: qualityMetricsLoading, children: [_jsx(Statistic, { title: "\u8865\u8D34\u6750\u6599\u9F50\u5957", value: qualityMetrics?.subsidyId == null
                                                            ? '未建单'
                                                            : qualityMetrics.subsidyTotalSlots
                                                                ? `${qualityMetrics.subsidyReadySlots ?? 0}/${qualityMetrics.subsidyTotalSlots}`
                                                                : qualityMetrics.subsidyReady
                                                                    ? '已齐套'
                                                                    : '—', valueStyle: {
                                                            color: qualityMetrics?.subsidyReady === true
                                                                ? '#3f8600'
                                                                : qualityMetrics?.subsidyReady === false
                                                                    ? '#cf1322'
                                                                    : undefined,
                                                        } }), _jsx(Button, { type: "link", size: "small", style: { padding: 0, marginTop: 4 }, onClick: () => void openClassSubsidy(), children: qualityMetrics?.subsidyId == null ? '为本班申报补贴' : '办理本班补贴' })] }) })] }), _jsx(Card, { size: "small", title: "\u8D28\u91CF\u9884\u8B66", loading: qualityMetricsLoading, children: (qualityMetrics?.alerts?.length ?? 0) === 0 ? (_jsx(Empty, { image: Empty.PRESENTED_IMAGE_SIMPLE, description: "\u6682\u65E0\u9884\u8B66" })) : (_jsx(Space, { direction: "vertical", style: { width: '100%' }, children: (qualityMetrics?.alerts ?? []).map((a, idx) => (_jsx(Alert, { type: a.level === 'critical' ? 'error' : 'warning', showIcon: true, message: a.message || a.code }, `${a.code}-${idx}`))) })) }), _jsx(Alert, { type: "info", showIcon: true, message: "\u53E3\u5F84\u8BF4\u660E", description: _jsxs(_Fragment, { children: ["\u5F53\u65E5\u51FA\u52E4=\u5B9E\u5230\u00F7\u540D\u5355\u4EBA\u6570\uFF1B\u7D2F\u8BA1\u51FA\u52E4=\u6709\u6548\u51FA\u52E4\u8BB0\u5F55\u00F7\u5168\u90E8\u8003\u52E4\u8BB0\u5F55\uFF08\u8986\u76D6 ", classStats.recordDays, ' ', "\u4E2A\u6709\u8003\u52E4\u65E5\uFF09\u3002\u8BA1\u5212\u5B66\u65F6=\u8BFE\u8868\u8D77\u6B62\u65F6\u95F4\u00F745 \u5206\u949F\uFF08\u65E0\u65F6\u95F4\u7528\u8BFE\u8868\u5B66\u65F6\uFF09\u3002\u5DF2\u4E0A\u8BFE\u65F6=\u4ECA\u65E5\u53CA\u4EE5\u524D\u8BFE\u6B21\u4E2D\u672A\u65F7\u8BFE\u7684\u8BFE\u65F6\u4E4B\u548C\uFF08\u672A\u6765\u8BFE\u6B21\u4E0D\u8BA1\uFF09\uFF1B\u672A\u767B\u8BB0\u89C6\u4E3A\u672A\u65F7\u8BFE\u3002\u8BFE\u65F6\u5B8C\u6210\u7387=\u5DF2\u4E0A\u8BFE\u65F6/\u5168\u8BFE\u8868\u8BA1\u5212\u5B66\u65F6\u3002\u4F5C\u4E1A\u5B8C\u6210\u7387=\u5DF2\u4EA4\u6709\u6548\u4EFD\u6570\u00F7\u5E94\u53D1\u4EFD\u6570\u3002\u6A21\u62DF\u901A\u8FC7\u7387=\u5DF2\u8BC4\u901A\u8FC7\u4EFD\u6570\u00F7\u5DF2\u8BC4\u4EFD\u6570\u3002\u8003\u8BD5\u901A\u8FC7\u7387=\u5F97\u5206\u2265\u53CA\u683C\u7EBF\u4EBA\u6570\u00F7\u6709\u6210\u7EE9\u4EBA\u6570\u3002\u6EE1\u610F\u5EA6=\u7B54\u5377\u5747\u5206\uFF081\uFF5E5\uFF09\u3002\u8865\u8D34\u9F50\u5957=\u914D\u65B9\u5FC5\u586B\u69FD\u4F4D\u5747\u5DF2\u6709\u6750\u6599\uFF08\u6216\u5DF2\u751F\u6210\u6750\u6599\u5305\uFF09\u3002 \u9884\u8B66\u9608\u503C\uFF1A\u7D2F\u8BA1\u51FA\u52E4/\u4F5C\u4E1A\u5B8C\u6210\u7387 <80%\u3001\u6EE1\u610F\u5EA6 <3.5\u3001\u5F71\u50CF\u672A\u9F50\u5957\u3001\u4E25\u91CD\u6574\u6539\u672A\u5173\u3001\u8865\u8D34\u7F3A\u9879\u3002"] }) })] })),
                    },
                ] }), _jsx(StudentAttendanceDetailDrawer, { open: !!detailStudent, classId: classIdNum, student: detailStudent, hoursRow: detailStudent ? hoursByPeopleId.get(String(detailStudent.id)) : undefined, onClose: () => setDetailStudent(null) }), _jsx(Modal, { title: "\u8003\u52E4\u5BFC\u5165\u7ED3\u679C", open: !!importResult, onCancel: () => setImportResult(null), onOk: () => setImportResult(null), width: 720, destroyOnHidden: true, children: importResult && (_jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: "middle", children: [_jsx(Alert, { type: (importResult.created + importResult.updated) > 0 ? 'success' : 'warning', showIcon: true, message: `文件行 ${importResult.totalRows} · 合并人日 ${importResult.parsedRows} · 新增 ${importResult.created} · 更新 ${importResult.updated} · 跳过行 ${importResult.skipped}`, description: "\u540C\u65E5\u591A\u8BFE\u6B21\u5DF2\u6309\u6700\u5DEE\u72B6\u6001\u5408\u5E76\uFF1B\u5207\u6362\u4E1A\u52A1\u65E5\u671F\u53EF\u67E5\u770B\u5BF9\u5E94\u5929\u7684\u8003\u52E4\u3002" }), (importResult.failures?.length ?? 0) > 0 && (_jsx(Table, { size: "small", rowKey: (r) => `${r.rowNum}-${r.idCard}-${r.attendanceDate}`, dataSource: importResult.failures, pagination: { pageSize: 8, showSizeChanger: false }, columns: [
                                { title: '行号', dataIndex: 'rowNum', width: 70 },
                                { title: '姓名', dataIndex: 'name', width: 100, render: (v) => v || '—' },
                                { title: '身份证', dataIndex: 'idCard', width: 180, render: (v) => v || '—' },
                                { title: '上课日期', dataIndex: 'attendanceDate', width: 110, render: (v) => v || '—' },
                                { title: '原因', dataIndex: 'reason' },
                            ] }))] })) }), _jsx(Modal, { title: editingScheduleKey ? '直接改课' : '添加课次', open: scheduleModalOpen, onCancel: () => setScheduleModalOpen(false), onOk: () => submitScheduleEditor(), confirmLoading: saveScheduleMu.isPending, destroyOnHidden: true, width: 520, children: _jsxs(Form, { form: scheduleForm, layout: "vertical", children: [_jsx(Form.Item, { name: "scheduleDate", label: "\u65E5\u671F", rules: [{ required: true, message: '请选择日期' }], children: _jsx(DatePicker, { style: { width: '100%' } }) }), _jsx(Form.Item, { name: "timeRange", label: "\u65F6\u95F4", rules: [{ required: true, message: '请选择起止时间' }], children: _jsx(TimePicker.RangePicker, { format: "HH:mm", style: { width: '100%' } }) }), _jsx(Form.Item, { name: "courseName", label: "\u57F9\u8BAD\u5185\u5BB9", rules: [{ required: true, message: '请输入培训内容' }], children: _jsx(Input, { placeholder: "\u57F9\u8BAD\u5185\u5BB9 / \u8BFE\u7A0B\u540D\u79F0" }) }), _jsx(Form.Item, { name: "classHours", label: "\u5B66\u65F6", children: _jsx(Input, { placeholder: "\u5982 2" }) }), _jsx(Form.Item, { name: "teachForm", label: "\u6388\u8BFE\u5F62\u5F0F", children: _jsx(Select, { allowClear: true, placeholder: "\u9009\u62E9\u6216\u7559\u7A7A", options: [
                                    { value: '理论', label: '理论' },
                                    { value: '实操', label: '实操' },
                                ] }) }), _jsx(Form.Item, { name: "teacherName", label: "\u8001\u5E08", children: _jsx(TeacherBankSelect, {}) })] }) })] }));
}

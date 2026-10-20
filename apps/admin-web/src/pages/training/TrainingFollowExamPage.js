import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useEffect, useMemo, useRef, useState } from 'react';
import { Alert, Breadcrumb, Button, Card, Col, Drawer, Empty, Input, Pagination, Row, Space, Spin, Table, Tabs, Tag, Typography, Upload, message, } from 'antd';
import { ArrowLeftOutlined, DownloadOutlined, RightOutlined, SearchOutlined, UploadOutlined, } from '@ant-design/icons';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import { useAuthStore } from '../../stores/authStore';
import { TrainingProcessSteps } from './TrainingProcessSteps';
import './TrainingFollowExamPage.css';
/** 班级工种等级：5=五级 … 1=一级（与开班准备一致） */
const CLASS_LEVEL_LABELS = {
    5: '五级/初级工',
    4: '四级/中级工',
    3: '三级/高级工',
    2: '二级/技师',
    1: '一级/高级技师',
};
function formatClassLevel(level) {
    if (level == null || Number.isNaN(Number(level)))
        return '—';
    const n = Number(level);
    return CLASS_LEVEL_LABELS[n] ?? `Lv${n}`;
}
const WRITABLE = new Set([
    'MATCHED',
    'MATCHED_NAME_WARN',
    'MATCHED_JOB_WARN',
    'MULTI_CLASS',
]);
const STATUS_LABEL = {
    MATCHED: { text: '可写入', color: 'success' },
    MATCHED_NAME_WARN: { text: '姓名不一致', color: 'warning' },
    MATCHED_JOB_WARN: { text: '工种/等级不一致', color: 'warning' },
    MULTI_CLASS: { text: '多班命中', color: 'processing' },
    UNMATCHED_NOT_IN_CLASS: { text: '未在本班' },
    NO_ID_CARD: { text: '无证件号', color: 'error' },
    EMPTY_SCORE: { text: '空分' },
    SHEET_DUP_CONFLICT: { text: '表内冲突', color: 'error' },
};
/** 班级/培训未挂企业时的虚拟入口 */
const UNASSIGNED_COMPANY_ID = '0';
/** 企业/组织雪花 ID：一律按字符串比较，禁止 Number() */
function toIdKey(value) {
    if (value == null || value === '')
        return null;
    const s = String(value).trim();
    if (!s || s === 'null' || s === 'undefined' || s === 'NaN')
        return null;
    if (s === '0')
        return null;
    return s;
}
function flattenWritable(items) {
    const rows = [];
    for (const item of items) {
        if (!WRITABLE.has(item.status))
            continue;
        for (const t of item.targets ?? []) {
            rows.push({
                key: String(t.peopleId),
                peopleId: String(t.peopleId),
                rowNo: item.rowNo,
                name: item.name,
                idCard: item.idCard,
                ticketNo: item.ticketNo,
                theoryScore: item.theoryScore,
                skillScore: item.skillScore,
                examScore: item.examScore,
                performanceScore: item.performanceScore,
                theoryFallback: item.theoryFallback,
                status: item.status,
                classTitle: t.classTitle,
                rosterName: t.rosterName,
                oldExamScore: t.oldExamScore,
                oldRealScore: t.oldRealScore,
                nameMismatch: t.nameMismatch,
                jobMismatch: t.jobMismatch,
                mergeNote: item.mergeNote,
                examTimeMissing: item.examTimeMissing,
            });
        }
    }
    return rows;
}
function statusTag(code) {
    const meta = STATUS_LABEL[code ?? ''] ?? { text: code || '-' };
    return _jsx(Tag, { color: meta.color, children: meta.text });
}
const PASS_LINE = 60;
function renderExamScoreCell(v) {
    if (v == null || v === '')
        return '—';
    const n = typeof v === 'number' ? v : Number(v);
    if (Number.isNaN(n))
        return v;
    if (n < PASS_LINE) {
        return _jsx("span", { className: "exam-score-fail", children: n });
    }
    return _jsx("span", { className: "exam-score-pass", children: n });
}
function examResultTag(status) {
    if (status === 'PASS')
        return _jsx(Tag, { color: "success", children: "\u5408\u683C" });
    if (status === 'FAIL')
        return _jsx(Tag, { color: "error", children: "\u4E0D\u5408\u683C" });
    if (status === 'ABSENT')
        return _jsx(Tag, { children: "\u7F3A\u8003" });
    return _jsx(Tag, { children: "\u2014" });
}
function examRosterRowClass(status) {
    if (status === 'PASS')
        return 'exam-roster-pass-row';
    if (status === 'FAIL')
        return 'exam-roster-fail-row';
    if (status === 'ABSENT')
        return 'exam-roster-absent-row';
    return '';
}
/** 百分比展示：分母为 0 时显示 — */
function formatRate(numerator, denominator, digits = 1) {
    if (denominator <= 0)
        return '—';
    const pct = (numerator / denominator) * 100;
    return `${pct.toFixed(digits)}%`;
}
function buildExamRosterRateSummary(stats) {
    const attended = stats.passed + stats.failed;
    return {
        /** 合格 / 合计 */
        passRate: formatRate(stats.passed, stats.total),
        /** 参考（有成绩） / 合计 */
        attendRate: formatRate(attended, stats.total),
        /** 缺考 / 合计 */
        absentRate: formatRate(stats.absent, stats.total),
        /** 合格 / 参考人数（剔除缺考） */
        passAmongAttended: formatRate(stats.passed, attended),
        theoryPassRate: formatRate(stats.theoryPass, stats.theoryPass + stats.theoryFail),
        skillPassRate: formatRate(stats.skillPass, stats.skillPass + stats.skillFail),
    };
}
export function TrainingFollowExamPage() {
    const queryClient = useQueryClient();
    const hasPerm = useAuthStore((s) => s.hasPermission);
    const loginRole = useAuthStore((s) => s.currentUser?.loginRole);
    const roles = useAuthStore((s) => s.currentUser?.roles ?? []);
    const username = useAuthStore((s) => s.currentUser?.username);
    const userType = useAuthStore((s) => s.currentUser?.userType);
    const currentOrgId = useAuthStore((s) => s.currentUser?.orgId);
    const canImportWrite = hasPerm('train:follow-exam:update')
        || hasPerm('train:exam-list:update')
        || hasPerm('train:people-list:update');
    const roleSet = useMemo(() => new Set((roles ?? []).map((r) => String(r).toUpperCase())), [roles]);
    const isPlatformAdmin = roleSet.has('PLATFORM_ADMIN')
        || roleSet.has('SUPER_ADMIN')
        || userType === 'admin'
        || username === 'admin';
    /** 与方案规划页一致：平台管理员即使挂在企业组织也不按企业视角收窄 */
    const isEnterpriseViewer = loginRole === 'company' && !isPlatformAdmin;
    const [companyKeyword, setCompanyKeyword] = useState('');
    const [activeCompanyId, setActiveCompanyId] = useState(null);
    const [classKeyword, setClassKeyword] = useState('');
    const [trainTitleFilters, setTrainTitleFilters] = useState([]);
    const [companyPage, setCompanyPage] = useState(1);
    const [companyPageSize, setCompanyPageSize] = useState(24);
    const [classPage, setClassPage] = useState(1);
    const [classPageSize, setClassPageSize] = useState(10);
    const [selectedClassIds, setSelectedClassIds] = useState([]);
    const [rosterClass, setRosterClass] = useState(null);
    const [rosterKeyword, setRosterKeyword] = useState('');
    const [rosterPage, setRosterPage] = useState(1);
    const [rosterPageSize, setRosterPageSize] = useState(50);
    const [rosterResultFilter, setRosterResultFilter] = useState(null);
    const [preview, setPreview] = useState(null);
    const [lastCommit, setLastCommit] = useState(null);
    const [importing, setImporting] = useState(false);
    const [committing, setCommitting] = useState(false);
    const [uploadFileName, setUploadFileName] = useState(null);
    const pendingUploadFilesRef = useRef([]);
    /** 与开班准备同源：企业/培训需求全量；班级用于统计已开班数 */
    const publicQuery = useQuery({
        queryKey: ['exam-score-import-public'],
        queryFn: () => trainApi.getDemandPublicData(['company', 'traininfo']),
    });
    const publicData = publicQuery.data;
    const allClassesQuery = useQuery({
        queryKey: ['exam-score-import-classes-all'],
        queryFn: () => trainApi.listExamScoreImportClasses({}),
    });
    const companyClassesQuery = useQuery({
        queryKey: ['exam-score-import-classes', activeCompanyId],
        queryFn: () => trainApi.listExamScoreImportClasses({
            companyId: activeCompanyId,
        }),
        enabled: activeCompanyId != null,
    });
    const classPeopleQuery = useQuery({
        queryKey: [
            'exam-score-import-class-people',
            rosterClass?.id,
            rosterPage,
            rosterPageSize,
            rosterKeyword,
            rosterResultFilter,
        ],
        queryFn: () => trainApi.pageExamScoreImportClassPeople(rosterClass.id, {
            page: rosterPage,
            pageSize: rosterPageSize,
            keyword: rosterKeyword.trim() || undefined,
            examResultStatus: rosterResultFilter || undefined,
        }),
        enabled: rosterClass != null,
    });
    const companies = useMemo(() => {
        const classCountByCompany = new Map();
        const trainInfoCompany = new Map();
        for (const t of publicData?.traininfo_list ?? []) {
            const tid = toIdKey(t.id);
            const cid = toIdKey(t.company_id);
            if (tid && cid) {
                trainInfoCompany.set(tid, cid);
            }
        }
        let unassignedClassCount = 0;
        for (const c of allClassesQuery.data ?? []) {
            let cid = toIdKey(c.companyId);
            if (!cid) {
                cid = trainInfoCompany.get(toIdKey(c.trainInfoId) ?? '') ?? null;
            }
            if (!cid) {
                unassignedClassCount += 1;
                continue;
            }
            classCountByCompany.set(cid, (classCountByCompany.get(cid) ?? 0) + 1);
        }
        const trainCountByCompany = new Map();
        let unassignedTrainCount = 0;
        for (const t of publicData?.traininfo_list ?? []) {
            const cid = toIdKey(t.company_id);
            if (!cid) {
                unassignedTrainCount += 1;
                continue;
            }
            trainCountByCompany.set(cid, (trainCountByCompany.get(cid) ?? 0) + 1);
        }
        const map = new Map();
        for (const c of publicData?.company_list ?? []) {
            const cid = toIdKey(c.id);
            if (!cid)
                continue;
            map.set(cid, {
                companyId: cid,
                companyName: (c.title || `企业#${cid}`).trim(),
                classCount: classCountByCompany.get(cid) ?? 0,
                trainCount: trainCountByCompany.get(cid) ?? 0,
            });
        }
        for (const [cid, count] of classCountByCompany) {
            if (map.has(cid))
                continue;
            const sample = (allClassesQuery.data ?? []).find((x) => toIdKey(x.companyId) === cid);
            map.set(cid, {
                companyId: cid,
                companyName: (sample?.companyName || `企业#${cid}`).trim(),
                classCount: count,
                trainCount: trainCountByCompany.get(cid) ?? 0,
            });
        }
        for (const [cid, count] of trainCountByCompany) {
            if (map.has(cid))
                continue;
            const sample = (publicData?.traininfo_list ?? []).find((x) => toIdKey(x.company_id) === cid);
            map.set(cid, {
                companyId: cid,
                companyName: (sample?.company_name || `企业#${cid}`).trim(),
                classCount: classCountByCompany.get(cid) ?? 0,
                trainCount: count,
            });
        }
        const rows = [...map.values()].sort((a, b) => a.companyName.localeCompare(b.companyName, 'zh-CN'));
        if (unassignedClassCount > 0 || unassignedTrainCount > 0) {
            // 企业账号不展示「未归属」入口，避免误导与越权尝试
            if (!isEnterpriseViewer) {
                rows.unshift({
                    companyId: UNASSIGNED_COMPANY_ID,
                    companyName: '未归属企业（请补全企业后重试）',
                    classCount: unassignedClassCount,
                    trainCount: unassignedTrainCount,
                    unassigned: true,
                });
            }
        }
        return rows;
    }, [publicData, allClassesQuery.data, isEnterpriseViewer]);
    const filteredCompanies = useMemo(() => {
        const kw = companyKeyword.trim().toLowerCase();
        let list = companies;
        if (isEnterpriseViewer) {
            const selfId = toIdKey(currentOrgId);
            list = list.filter((c) => (selfId ? c.companyId === selfId : false));
        }
        if (kw) {
            list = list.filter((c) => c.companyName.toLowerCase().includes(kw) || c.companyId.includes(kw));
        }
        // 机构/平台管理员保留完整企业目录；仅企业视角可再收窄（本企业通常就一家）
        return list;
    }, [companies, companyKeyword, isEnterpriseViewer, currentOrgId]);
    const pagedCompanies = useMemo(() => {
        const start = (companyPage - 1) * companyPageSize;
        return filteredCompanies.slice(start, start + companyPageSize);
    }, [filteredCompanies, companyPage, companyPageSize]);
    /** 优先用企业筛选接口；若因精度问题为空则回退本地按字符串归类 */
    const companyClasses = useMemo(() => {
        const fromApi = companyClassesQuery.data ?? [];
        if (activeCompanyId == null)
            return [];
        if (fromApi.length > 0)
            return fromApi;
        const trainInfoCompany = new Map();
        for (const t of publicData?.traininfo_list ?? []) {
            const tid = toIdKey(t.id);
            const cid = toIdKey(t.company_id);
            if (tid && cid)
                trainInfoCompany.set(tid, cid);
        }
        return (allClassesQuery.data ?? []).filter((c) => {
            let cid = toIdKey(c.companyId);
            if (!cid)
                cid = trainInfoCompany.get(toIdKey(c.trainInfoId) ?? '') ?? null;
            if (activeCompanyId === UNASSIGNED_COMPANY_ID) {
                return !cid;
            }
            return cid === activeCompanyId;
        });
    }, [activeCompanyId, companyClassesQuery.data, allClassesQuery.data, publicData]);
    const trainTitleFilterOptions = useMemo(() => {
        const set = new Set();
        let hasEmpty = false;
        for (const c of companyClasses) {
            const t = (c.trainTitle || '').trim();
            if (!t) {
                hasEmpty = true;
                continue;
            }
            set.add(t);
        }
        const opts = [...set].sort((a, b) => a.localeCompare(b, 'zh-CN')).map((t) => ({
            text: t,
            value: t,
        }));
        if (hasEmpty) {
            opts.push({ text: '（未关联项目）', value: '__EMPTY__' });
        }
        return opts;
    }, [companyClasses]);
    const filteredClasses = useMemo(() => {
        let list = companyClasses;
        if (trainTitleFilters.length > 0) {
            const allowEmpty = trainTitleFilters.includes('__EMPTY__');
            const titles = new Set(trainTitleFilters.filter((t) => t !== '__EMPTY__'));
            list = list.filter((c) => {
                const t = (c.trainTitle || '').trim();
                if (!t)
                    return allowEmpty;
                return titles.has(t);
            });
        }
        const kw = classKeyword.trim().toLowerCase();
        if (!kw)
            return list;
        return list.filter((c) => {
            const blob = [c.title, c.trainTitle, c.workTypeTitle].filter(Boolean).join(' ').toLowerCase();
            return blob.includes(kw);
        });
    }, [companyClasses, classKeyword, trainTitleFilters]);
    const activeCompany = useMemo(() => companies.find((c) => c.companyId === activeCompanyId) ?? null, [companies, activeCompanyId]);
    const writableRows = useMemo(() => flattenWritable(preview?.items ?? []), [preview]);
    const unmatchedItems = useMemo(() => (preview?.items ?? []).filter((i) => i.status === 'UNMATCHED_NOT_IN_CLASS' || i.status === 'NO_ID_CARD'), [preview]);
    const conflictItems = useMemo(() => (preview?.items ?? []).filter((i) => i.status === 'SHEET_DUP_CONFLICT'), [preview]);
    const emptyScoreItems = useMemo(() => (preview?.items ?? []).filter((i) => i.status === 'EMPTY_SCORE'), [preview]);
    const enterCompany = (companyId) => {
        setActiveCompanyId(companyId);
        setClassKeyword('');
        setTrainTitleFilters([]);
        setClassPage(1);
        setPreview(null);
        setLastCommit(null);
        setUploadFileName(null);
        setSelectedClassIds([]);
    };
    const backToCompanies = () => {
        setActiveCompanyId(null);
        setPreview(null);
        setLastCommit(null);
        setUploadFileName(null);
        setSelectedClassIds([]);
        setClassKeyword('');
        setTrainTitleFilters([]);
        setClassPage(1);
        setCompanyPage(1);
    };
    useEffect(() => {
        if (activeCompanyId == null)
            return;
        if (!companyClassesQuery.isSuccess)
            return;
        if (companyClasses.length === 0) {
            setSelectedClassIds([]);
            return;
        }
        setSelectedClassIds((prev) => prev.length === 0 ? companyClasses.map((c) => String(c.id)) : prev);
    }, [activeCompanyId, companyClasses, companyClassesQuery.isSuccess]);
    const targetClassIds = selectedClassIds;
    const canConfirmWrite = canImportWrite
        && targetClassIds.length > 0
        && writableRows.length > 0
        && preview != null
        && !committing
        && !importing;
    const runPreviewImport = async (files) => {
        if (!canImportWrite) {
            message.warning('当前角色无成绩导入权限');
            return;
        }
        if (targetClassIds.length === 0) {
            message.warning('请先勾选班级后再上传成绩表');
            return;
        }
        const list = files.filter(Boolean);
        if (list.length === 0) {
            message.warning('请选择成绩表文件');
            return;
        }
        setImporting(true);
        setPreview(null);
        setLastCommit(null);
        setUploadFileName(list.length === 1 ? list[0].name : `${list[0].name} 等${list.length}个文件`);
        try {
            const data = await trainApi.previewExamScoreImport(targetClassIds, list);
            setPreview(data);
            const peopleIds = flattenWritable(data.items).map((r) => r.peopleId);
            if (peopleIds.length === 0) {
                message.warning('没有可写入的成绩行，请查看匹配明细后再确认');
            }
            else {
                message.success(`已解析 ${list.length} 个成绩表，可写入 ${peopleIds.length} 人，请确认后写入`);
            }
        }
        catch (e) {
            setUploadFileName(null);
            message.error(e.message || '解析失败');
        }
        finally {
            setImporting(false);
        }
    };
    const runConfirmWrite = async () => {
        if (!preview)
            return;
        if (targetClassIds.length === 0) {
            message.warning('请先勾选班级');
            return;
        }
        const peopleIds = writableRows.map((r) => r.peopleId);
        if (peopleIds.length === 0) {
            message.warning('没有可写入的成绩行');
            return;
        }
        setCommitting(true);
        try {
            const res = await trainApi.commitExamScoreImport(preview.previewId, peopleIds);
            setLastCommit({
                written: res.written,
                overwritten: res.overwritten,
                skipped: res.skipped,
            });
            message.success(`写入完成：成功 ${res.written} 人，覆盖 ${res.overwritten}，跳过 ${res.skipped}`);
            void queryClient.invalidateQueries({ queryKey: ['exam-score-import-classes'] });
            void queryClient.invalidateQueries({ queryKey: ['exam-score-import-classes-all'] });
            if (rosterClass != null) {
                void classPeopleQuery.refetch();
            }
        }
        catch (e) {
            message.error(e.message || '写入失败');
        }
        finally {
            setCommitting(false);
        }
    };
    const handleDownloadTemplate = async () => {
        try {
            const blob = await trainApi.downloadExamScoreImportTemplate();
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = '成绩导入标准模板.xlsx';
            a.click();
            URL.revokeObjectURL(url);
        }
        catch {
            message.error('模板下载失败');
        }
    };
    const openClassRoster = (row) => {
        setRosterKeyword('');
        setRosterPage(1);
        setRosterResultFilter(null);
        setRosterClass(row);
    };
    const classColumns = [
        {
            title: '班级',
            width: 140,
            dataIndex: 'title',
            ellipsis: true,
            render: (v, row) => (_jsx(Typography.Link, { children: v || `班级#${row.id}` })),
        },
        {
            title: '工种',
            width: 120,
            ellipsis: true,
            render: (_, r) => r.workTypeTitle || '—',
        },
        {
            title: '等级',
            width: 120,
            ellipsis: true,
            render: (_, r) => formatClassLevel(r.workTypeLevel),
        },
        {
            title: '人数',
            dataIndex: 'studentCount',
            width: 64,
            align: 'right',
            render: (v) => (v == null ? '—' : v),
        },
        {
            title: '成绩导入',
            width: 110,
            render: (_, r) => {
                const total = r.studentCount ?? 0;
                const scored = r.scoredCount ?? 0;
                const status = r.scoreImportStatus ?? 'NONE';
                if (status === 'DONE') {
                    return _jsxs(Tag, { color: "success", children: ["\u5DF2\u5BFC\u5165 ", scored, "/", total] });
                }
                if (status === 'PARTIAL') {
                    return _jsxs(Tag, { color: "warning", children: ["\u90E8\u5206 ", scored, "/", total] });
                }
                return _jsx(Tag, { children: "\u672A\u5BFC\u5165" });
            },
        },
        {
            title: '通过',
            dataIndex: 'passedCount',
            width: 64,
            align: 'right',
            render: (v) => v == null ? '—' : (_jsx(Typography.Text, { type: "success", children: v })),
        },
        {
            title: '通过率',
            width: 80,
            align: 'right',
            render: (_, r) => {
                const total = r.studentCount ?? 0;
                const passed = r.passedCount ?? 0;
                if (total <= 0)
                    return '—';
                return (_jsxs(Typography.Text, { type: "success", strong: true, children: [((passed / total) * 100).toFixed(1), "%"] }));
            },
        },
        {
            title: '缺考',
            dataIndex: 'absentCount',
            width: 64,
            align: 'right',
            render: (v) => (v == null ? '—' : v),
        },
        {
            title: '不合格',
            dataIndex: 'failedCount',
            width: 72,
            align: 'right',
            render: (v) => v == null ? '—' : (_jsx(Typography.Text, { type: "danger", children: v })),
        },
        {
            title: '培训项目',
            width: 140,
            dataIndex: 'trainTitle',
            ellipsis: true,
            filters: trainTitleFilterOptions,
            filteredValue: trainTitleFilters.length > 0 ? trainTitleFilters : null,
            filterSearch: true,
            onFilter: () => true,
            render: (v) => v || '—',
        },
    ];
    const peopleColumns = [
        {
            title: '准考证号',
            dataIndex: 'ticketNo',
            width: 158,
            ellipsis: true,
            render: (v) => v || '—',
        },
        { title: '姓名', dataIndex: 'realname', width: 60, render: (v) => v || '—' },
        { title: '证件号码', dataIndex: 'idCard', width: 140, render: (v) => v || '—' },
        { title: '手机', dataIndex: 'phone', width: 120, render: (v) => v || '—' },
        {
            title: '理论',
            dataIndex: 'theoryScore',
            width: 80,
            render: (v) => renderExamScoreCell(v),
        },
        {
            title: '技能',
            dataIndex: 'skillScore',
            width: 80,
            render: (v) => renderExamScoreCell(v),
        },
        {
            title: '综合',
            dataIndex: 'examScore',
            width: 80,
            render: (v) => (v == null || v === '' ? '—' : v),
        },
        {
            title: '工作业绩',
            dataIndex: 'performanceScore',
            width: 90,
            render: (v) => (v == null || v === '' ? '—' : v),
        },
        {
            title: '结果',
            dataIndex: 'examResultStatus',
            width: 100,
            fixed: 'right',
            filters: [
                { text: '合格', value: 'PASS' },
                { text: '不合格', value: 'FAIL' },
                { text: '缺考', value: 'ABSENT' },
            ],
            filteredValue: rosterResultFilter ? [rosterResultFilter] : null,
            filterMultiple: false,
            onFilter: () => true,
            render: (v) => examResultTag(v),
        },
    ];
    const writableColumns = [
        { title: '行号', dataIndex: 'rowNo', width: 70 },
        { title: '证件号码', dataIndex: 'idCard', width: 170 },
        { title: '成绩单姓名', dataIndex: 'name', width: 100 },
        { title: '花名册姓名', dataIndex: 'rosterName', width: 100 },
        {
            title: '准考证号',
            dataIndex: 'ticketNo',
            width: 140,
            ellipsis: true,
            render: (v) => v || '—',
        },
        { title: '班级', dataIndex: 'classTitle', width: 120, ellipsis: true },
        {
            title: '理论',
            dataIndex: 'theoryScore',
            width: 80,
            render: (v) => v ?? '—',
        },
        {
            title: '技能',
            dataIndex: 'skillScore',
            width: 80,
            render: (v) => v ?? '—',
        },
        {
            title: '综合',
            dataIndex: 'examScore',
            width: 100,
            render: (v) => (v == null || v === '' ? '—' : v),
        },
        {
            title: '工作业绩',
            dataIndex: 'performanceScore',
            width: 100,
            render: (v) => v ?? '—',
        },
        {
            title: '合并',
            dataIndex: 'mergeNote',
            width: 140,
            ellipsis: true,
            render: (v, r) => v ? (_jsxs("span", { children: [v, r.examTimeMissing ? _jsx(Tag, { style: { marginLeft: 4 }, children: "\u65F6\u95F4\u7F3A\u5931" }) : null] })) : ('—'),
        },
        { title: '状态', dataIndex: 'status', width: 130, render: (v) => statusTag(v) },
    ];
    const sheetColumns = [
        { title: '行号', dataIndex: 'rowNo', width: 70 },
        { title: '姓名', dataIndex: 'name', width: 100 },
        { title: '证件号码', dataIndex: 'idCard', width: 170 },
        {
            title: '准考证号',
            dataIndex: 'ticketNo',
            width: 140,
            ellipsis: true,
            render: (v) => v || '—',
        },
        { title: '理论', dataIndex: 'theoryScore', width: 80, render: (v) => v ?? '—' },
        { title: '技能', dataIndex: 'skillScore', width: 80, render: (v) => v ?? '—' },
        { title: '综合', dataIndex: 'examScore', width: 80, render: (v) => v ?? '—' },
        { title: '工作业绩', dataIndex: 'performanceScore', width: 100, render: (v) => v ?? '—' },
        { title: '状态', dataIndex: 'status', width: 130, render: (v) => statusTag(v) },
    ];
    const gapColumns = [
        { title: '班级', dataIndex: 'classTitle' },
        { title: '花名册姓名', dataIndex: 'rosterName', width: 120 },
        { title: '证件号码', dataIndex: 'idCard', width: 180 },
    ];
    return (_jsxs("div", { className: "training-page training-follow-exam-page", children: [_jsx(TrainingProcessSteps, { current: 6 }), _jsxs("section", { className: "page-heading", children: [_jsx(Typography.Title, { level: 4, style: { marginBottom: 4 }, children: "\u8003\u8BD5\u6210\u7EE9\u5BFC\u5165" }), _jsx(Typography.Text, { type: "secondary", children: "\u5148\u9009\u4F01\u4E1A\u4E0E\u73ED\u7EA7\uFF0C\u4E0A\u4F20\u6210\u7EE9\u8868\u540E\u6838\u5BF9\u5339\u914D\u660E\u7EC6\uFF0C\u518D\u786E\u8BA4\u5199\u5165\u82B1\u540D\u518C\u4F9B\u7ED3\u4E1A\u4F7F\u7528\u3002" })] }), _jsx(Alert, { type: "info", showIcon: true, style: { marginBottom: 16 }, message: "\u5199\u5165\u5F00\u73ED\u82B1\u540D\u518C\u6210\u7EE9\uFF0C\u4F9B\u7ED3\u4E1A\u4F7F\u7528\uFF1B\u8DDF\u73ED\u6A21\u62DF\u8003\u5206\u6570\u4E0D\u53D8\u3002\u4E0A\u4F20\u4EC5\u505A\u5339\u914D\u9884\u89C8\uFF0C\u9700\u70B9\u300C\u786E\u8BA4\u5199\u5165\u300D\u624D\u4F1A\u843D\u5E93\u3002\u672A\u52FE\u9009\u73ED\u7EA7\u65F6\u4E0D\u53EF\u5BFC\u5165/\u786E\u8BA4\u3002\u82E5\u73ED\u7EA7\u5728\u300C\u672A\u5F52\u5C5E\u4F01\u4E1A\u300D\uFF0C\u8BF7\u5148\u5230\u5F00\u73ED\u51C6\u5907\u4E3A\u57F9\u8BAD\u9700\u6C42\u9009\u62E9\u4F01\u4E1A\u3002" }), activeCompanyId == null ? (_jsxs(Card, { size: "small", title: "\u5168\u90E8\u4F01\u4E1A", extra: _jsx(Input, { allowClear: true, prefix: _jsx(SearchOutlined, {}), placeholder: "\u641C\u7D22\u4F01\u4E1A", value: companyKeyword, onChange: (e) => {
                        setCompanyKeyword(e.target.value);
                        setCompanyPage(1);
                    }, style: { width: 220 } }), children: [_jsx(Spin, { spinning: publicQuery.isLoading || allClassesQuery.isLoading, children: filteredCompanies.length === 0 ? (_jsx(Empty, { description: "\u6682\u65E0\u4F01\u4E1A\u6570\u636E" })) : (_jsx(Row, { gutter: [16, 16], className: "exam-company-card-grid", children: pagedCompanies.map((row) => (_jsx(Col, { xs: 24, sm: 12, md: 8, lg: 6, children: _jsx("button", { type: "button", className: `exam-company-card${row.unassigned ? ' is-unassigned' : ''}${row.classCount > 0 ? ' has-classes' : ''}`, onClick: () => enterCompany(row.companyId), children: _jsxs("div", { className: "exam-company-card-body", children: [_jsx(Typography.Text, { className: "exam-company-card-title", ellipsis: { tooltip: row.companyName }, type: row.unassigned ? 'warning' : undefined, children: row.companyName }), _jsxs("div", { className: "exam-company-card-stats", children: [_jsxs("span", { children: [_jsx("em", { children: row.classCount }), " \u4E2A\u73ED"] }), _jsxs("span", { children: [_jsx("em", { children: row.trainCount }), " \u4E2A\u9879\u76EE"] })] }), _jsxs(Typography.Text, { type: "secondary", className: "exam-company-card-action", children: ["\u8FDB\u5165\u6210\u7EE9\u5BFC\u5165 ", _jsx(RightOutlined, {})] })] }) }) }, row.companyId))) })) }), filteredCompanies.length > 0 ? (_jsx("div", { className: "exam-company-card-pagination", children: _jsx(Pagination, { current: companyPage, pageSize: companyPageSize, total: filteredCompanies.length, showSizeChanger: true, pageSizeOptions: ['12', '24', '48', '96'], showTotal: (n) => `共 ${n} 家企业`, onChange: (page, size) => {
                                setCompanyPage(page);
                                if (typeof size === 'number' && size > 0) {
                                    setCompanyPageSize(size);
                                }
                            } }) })) : null] })) : (_jsxs(Space, { direction: "vertical", size: 16, style: { width: '100%' }, children: [_jsx(Breadcrumb, { items: [
                            {
                                title: (_jsxs("a", { onClick: backToCompanies, children: [_jsx(ArrowLeftOutlined, {}), " \u5168\u90E8\u4F01\u4E1A"] })),
                            },
                            { title: activeCompany?.companyName || `企业#${activeCompanyId}` },
                        ] }), _jsxs(Card, { size: "small", title: `班级列表（${companyClasses.length}）`, loading: companyClassesQuery.isLoading, extra: _jsxs(Space, { wrap: true, size: 8, children: [_jsx(Input, { allowClear: true, prefix: _jsx(SearchOutlined, {}), placeholder: "\u641C\u7D22\u73ED\u7EA7/\u9879\u76EE", value: classKeyword, onChange: (e) => {
                                        setClassKeyword(e.target.value);
                                        setClassPage(1);
                                    }, style: { width: 180 } }), _jsx(Button, { size: "small", onClick: () => setSelectedClassIds(companyClasses.map((c) => String(c.id))), children: "\u5168\u9009\u73ED\u7EA7" }), _jsx(Button, { size: "small", icon: _jsx(DownloadOutlined, {}), onClick: handleDownloadTemplate, children: "\u6807\u51C6\u6A21\u677F" }), canImportWrite ? (_jsxs(_Fragment, { children: [_jsx(Upload, { accept: ".xlsx,.xls", multiple: true, maxCount: 20, showUploadList: false, disabled: targetClassIds.length === 0 || importing || committing, beforeUpload: (file, fileList) => {
                                                pendingUploadFilesRef.current.push(file);
                                                if (pendingUploadFilesRef.current.length >= fileList.length) {
                                                    const batch = [...pendingUploadFilesRef.current];
                                                    pendingUploadFilesRef.current = [];
                                                    void runPreviewImport(batch);
                                                }
                                                return false;
                                            }, children: _jsx(Button, { size: "small", type: "primary", icon: _jsx(UploadOutlined, {}), loading: importing, disabled: targetClassIds.length === 0 || committing, children: "\u5BFC\u5165\u6210\u7EE9" }) }), _jsx(Button, { size: "small", type: "primary", ghost: true, loading: committing, disabled: !canConfirmWrite, onClick: () => void runConfirmWrite(), children: "\u786E\u8BA4\u5199\u5165" })] })) : (_jsx(Typography.Text, { type: "secondary", style: { fontSize: 12 }, children: "\u5F53\u524D\u89D2\u8272\u4EC5\u53EF\u67E5\u770B\uFF0C\u65E0\u6210\u7EE9\u5BFC\u5165\u6743\u9650" }))] }), children: [_jsx(Table, { size: "small", rowKey: (r) => String(r.id), columns: classColumns, dataSource: filteredClasses, scroll: { x: 900 }, pagination: {
                                    current: classPage,
                                    pageSize: classPageSize,
                                    showSizeChanger: true,
                                    pageSizeOptions: ['10', '20', '50', '100'],
                                    showTotal: (n) => `共 ${n} 个班`,
                                }, onChange: (pagination, filters) => {
                                    const raw = filters.trainTitle;
                                    const next = Array.isArray(raw)
                                        ? raw.map(String).filter(Boolean)
                                        : [];
                                    const filtersChanged = next.length !== trainTitleFilters.length
                                        || next.some((v, i) => v !== trainTitleFilters[i]);
                                    if (filtersChanged) {
                                        setTrainTitleFilters(next);
                                        setClassPage(1);
                                    }
                                    else if (pagination.current != null) {
                                        setClassPage(pagination.current);
                                    }
                                    if (typeof pagination.pageSize === 'number'
                                        && pagination.pageSize > 0
                                        && pagination.pageSize !== classPageSize) {
                                        setClassPageSize(pagination.pageSize);
                                        setClassPage(1);
                                    }
                                }, onRow: (row) => ({
                                    style: { cursor: 'pointer' },
                                    onClick: (e) => {
                                        const el = e.target;
                                        if (el?.closest('.ant-checkbox-wrapper, .ant-checkbox, input, button, .ant-table-filter-trigger, .ant-dropdown')) {
                                            return;
                                        }
                                        openClassRoster(row);
                                    },
                                }), rowSelection: {
                                    selectedRowKeys: selectedClassIds,
                                    onChange: (keys) => {
                                        setSelectedClassIds(keys.map(String));
                                        setPreview(null);
                                        setLastCommit(null);
                                        setUploadFileName(null);
                                    },
                                } }), _jsxs(Typography.Paragraph, { type: "secondary", style: { marginTop: 8, marginBottom: 0 }, children: ["\u5DF2\u9009 ", targetClassIds.length, " \u4E2A\u73ED\u53C2\u4E0E\u5339\u914D", uploadFileName ? `；当前成绩表：${uploadFileName}` : '；可多选成绩表上传，再点确认写入', targetClassIds.length === 0 ? '（未选班级时不可导入/确认）' : ''] }), lastCommit ? (_jsx(Alert, { style: { marginTop: 12 }, type: "success", showIcon: true, message: `本次写入：成功 ${lastCommit.written}，覆盖 ${lastCommit.overwritten}，跳过 ${lastCommit.skipped}` })) : null] }), preview && canImportWrite ? (_jsxs(Card, { size: "small", title: "\u5339\u914D\u660E\u7EC6\uFF08\u786E\u8BA4\u540E\u5199\u5165\uFF09", extra: _jsx(Button, { type: "primary", loading: committing, disabled: !canConfirmWrite, onClick: () => void runConfirmWrite(), children: "\u786E\u8BA4\u5199\u5165" }), children: [_jsxs(Space, { wrap: true, style: { marginBottom: 12 }, children: [preview.layout === 'RENSHE_ROSTER' && preview.planName ? (_jsxs(Tag, { color: "blue", children: ["\u8BA1\u5212\uFF1A", preview.planName] })) : null, preview.planCode ? _jsxs(Tag, { children: ["\u7F16\u7801\uFF1A", preview.planCode] }) : null, preview.examTimeText ? _jsx(Tag, { children: preview.examTimeText }) : null, _jsxs("span", { children: ["\u6210\u7EE9\u5355 ", preview.totalRows, " \u4EBA"] }), _jsxs("span", { children: ["\u53EF\u5199 ", preview.matched] }), _jsxs("span", { children: ["\u59D3\u540D\u9EC4\u6807 ", preview.nameWarn] }), _jsxs("span", { children: ["\u672A\u5728\u672C\u73ED ", preview.unmatched] }), _jsxs("span", { children: ["\u672C\u73ED\u672A\u51FA\u73B0 ", preview.classMissingOnSheet] }), _jsxs("span", { children: ["\u7F3A\u8BC1 ", preview.classMissingIdCard] }), _jsxs("span", { children: ["\u540C\u573A\u51B2\u7A81 ", preview.sheetDup] }), _jsxs("span", { children: ["\u7A7A\u5206 ", preview.emptyScore] })] }), _jsx(Typography.Paragraph, { type: "secondary", style: { marginBottom: 12 }, children: "\u591A\u573A\u6210\u7EE9\u6309\u8003\u8BD5\u65F6\u95F4\u5408\u5E76\uFF08\u7A7A\u4E0D\u8986\u76D6\uFF09\uFF1A\u8865\u8003\u65E0\u5B9E\u64CD\u65F6\u7EE7\u627F\u521D\u8003\u6280\u80FD\uFF1B\u4EC5\u540C\u4E00\u8003\u8BD5\u65F6\u95F4\u5185\u5206\u503C\u4E0D\u4E00\u81F4\u624D\u8FDB\u300C\u540C\u573A\u51B2\u7A81\u300D\u3002" }), _jsx(Tabs, { items: [
                                    {
                                        key: 'writable',
                                        label: `待写入 (${writableRows.length})`,
                                        children: (_jsx(Table, { size: "small", rowKey: "key", columns: writableColumns, dataSource: writableRows, pagination: false, scroll: { x: 1280 }, rowClassName: (r) => r.nameMismatch ||
                                                r.jobMismatch ||
                                                r.status === 'MATCHED_NAME_WARN' ||
                                                r.status === 'MATCHED_JOB_WARN'
                                                ? 'exam-score-import-warn-row'
                                                : '' })),
                                    },
                                    {
                                        key: 'unmatched',
                                        label: `未在本班 (${unmatchedItems.length})`,
                                        children: (_jsx(Table, { size: "small", rowKey: (r) => `${r.rowNo}-${r.idCard ?? r.name}`, columns: sheetColumns, dataSource: unmatchedItems, pagination: false })),
                                    },
                                    {
                                        key: 'missing',
                                        label: `本班未出现 (${preview.classMissingPeople.length})`,
                                        children: (_jsx(Table, { size: "small", rowKey: (r) => String(r.peopleId), columns: gapColumns, dataSource: preview.classMissingPeople, pagination: false })),
                                    },
                                    {
                                        key: 'conflict',
                                        label: `同场冲突 (${conflictItems.length})`,
                                        children: (_jsx(Table, { size: "small", rowKey: (r) => `${r.rowNo}-${r.idCard ?? r.name}-c`, columns: sheetColumns, dataSource: conflictItems, pagination: false })),
                                    },
                                    {
                                        key: 'empty',
                                        label: `空分 (${emptyScoreItems.length})`,
                                        children: (_jsx(Table, { size: "small", rowKey: (r) => `${r.rowNo}-${r.idCard ?? r.name}-e`, columns: sheetColumns, dataSource: emptyScoreItems, pagination: false })),
                                    },
                                    {
                                        key: 'no-id',
                                        label: `班内缺证 (${preview.missingIdCardPeople.length})`,
                                        children: (_jsx(Table, { size: "small", rowKey: (r) => String(r.peopleId), columns: gapColumns, dataSource: preview.missingIdCardPeople, pagination: false })),
                                    },
                                ] })] })) : null] })), _jsx(Drawer, { title: rosterClass
                    ? `${rosterClass.title || `班级#${rosterClass.id}`} · 人员考试信息`
                    : '人员考试信息', width: 860, open: rosterClass != null, onClose: () => {
                    setRosterClass(null);
                    setRosterResultFilter(null);
                    setRosterKeyword('');
                    setRosterPage(1);
                }, destroyOnClose: true, rootClassName: "training-follow-exam-roster-drawer", children: rosterClass ? (_jsxs(Space, { direction: "vertical", size: 12, style: { width: '100%' }, children: [_jsx(Typography.Text, { type: "secondary", children: [rosterClass.trainTitle, rosterClass.workTypeTitle, rosterClass.workTypeLevel != null ? `Lv${rosterClass.workTypeLevel}` : null]
                                .filter(Boolean)
                                .join(' · ') || '—' }), classPeopleQuery.data?.stats ? ((() => {
                            const stats = classPeopleQuery.data.stats;
                            const rates = buildExamRosterRateSummary(stats);
                            return (_jsxs("div", { className: "exam-roster-stats", children: [_jsxs("span", { className: "stat-item", children: ["\u5408\u8BA1 ", _jsx(Typography.Text, { strong: true, children: stats.total })] }), _jsxs("span", { className: "stat-item", children: ["\u5408\u683C", ' ', _jsx(Typography.Text, { type: "success", strong: true, children: stats.passed })] }), _jsxs("span", { className: "stat-item", children: ["\u4E0D\u5408\u683C", ' ', _jsx(Typography.Text, { type: "danger", strong: true, children: stats.failed })] }), _jsxs("span", { className: "stat-item", children: ["\u7F3A\u8003 ", _jsx(Typography.Text, { strong: true, children: stats.absent })] }), _jsx(Typography.Text, { type: "secondary", children: "|" }), _jsxs("span", { className: "stat-item", children: ["\u901A\u8FC7\u7387", ' ', _jsx(Typography.Text, { type: "success", strong: true, children: rates.passRate })] }), _jsxs("span", { className: "stat-item", children: ["\u53C2\u8003\u901A\u8FC7\u7387 ", _jsx(Typography.Text, { strong: true, children: rates.passAmongAttended })] }), _jsxs("span", { className: "stat-item", children: ["\u53C2\u8003\u7387 ", _jsx(Typography.Text, { strong: true, children: rates.attendRate })] }), _jsxs("span", { className: "stat-item", children: ["\u7F3A\u8003\u7387 ", _jsx(Typography.Text, { strong: true, children: rates.absentRate })] }), _jsx(Typography.Text, { type: "secondary", children: "|" }), _jsxs("span", { className: "stat-item", children: ["\u7406\u8BBA \u8FC7", stats.theoryPass, "/\u4E0D", stats.theoryFail, "/\u7F3A", stats.theoryAbsent, "\uFF08\u53CA\u683C\u7387 ", rates.theoryPassRate, "\uFF09"] }), _jsxs("span", { className: "stat-item", children: ["\u6280\u80FD \u8FC7", stats.skillPass, "/\u4E0D", stats.skillFail, "/\u7F3A", stats.skillAbsent, "\uFF08\u53CA\u683C\u7387 ", rates.skillPassRate, "\uFF09"] })] }));
                        })()) : null, _jsx(Typography.Paragraph, { type: "secondary", style: { marginBottom: 0 }, children: "\u5408\u683C\u6807\u51C6\uFF1A\u7406\u8BBA\u226560 \u4E14 \u6280\u80FD\u226560\uFF1B\u7406\u8BBA\u4E0E\u6280\u80FD\u5747\u65E0\u6210\u7EE9\u8BB0\u4E3A\u7F3A\u8003\u3002\u901A\u8FC7\u7387=\u5408\u683C/\u5408\u8BA1\uFF1B\u53C2\u8003\u901A\u8FC7\u7387=\u5408\u683C/(\u5408\u683C+\u4E0D\u5408\u683C)\uFF1B\u53C2\u8003\u7387=(\u5408\u683C+\u4E0D\u5408\u683C)/\u5408\u8BA1\u3002\u884C\u5E95\u8272\uFF1A\u7EFF=\u5408\u683C\u3001\u7EA2=\u4E0D\u5408\u683C\u3001\u7070=\u7F3A\u8003\u3002" }), _jsx(Input, { allowClear: true, prefix: _jsx(SearchOutlined, {}), placeholder: "\u641C\u7D22\u59D3\u540D / \u8BC1\u4EF6\u53F7 / \u624B\u673A", value: rosterKeyword, onChange: (e) => {
                                setRosterKeyword(e.target.value);
                                setRosterPage(1);
                            }, style: { maxWidth: 320 } }), _jsx(Table, { size: "small", rowKey: (r) => String(r.id), loading: classPeopleQuery.isLoading, columns: peopleColumns, dataSource: classPeopleQuery.data?.items ?? [], rowClassName: (r) => examRosterRowClass(r.examResultStatus), onChange: (pagination, filters) => {
                                const raw = filters.examResultStatus;
                                const next = Array.isArray(raw) && raw.length > 0 ? String(raw[0]) : null;
                                if (next !== rosterResultFilter) {
                                    setRosterResultFilter(next);
                                    setRosterPage(1);
                                }
                                else if (pagination.current != null) {
                                    setRosterPage(pagination.current);
                                }
                                if (pagination.pageSize != null && pagination.pageSize !== rosterPageSize) {
                                    setRosterPageSize(pagination.pageSize);
                                    setRosterPage(1);
                                }
                            }, pagination: {
                                current: rosterPage,
                                pageSize: rosterPageSize,
                                total: classPeopleQuery.data?.total ?? 0,
                                showSizeChanger: true,
                                showTotal: (n) => `共 ${n} 人`,
                            }, scroll: { x: 1000 } })] })) : null })] }));
}

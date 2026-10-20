import { jsxs as _jsxs, jsx as _jsx, Fragment as _Fragment } from "react/jsx-runtime";
/**
 * 分班 — 自动/手动创建班级草稿 → 企业审核通过后建班
 * 选规划 → 一键自动或手动组班 → 编辑草稿 → 提交企业审核
 */
import { useMemo, useRef, useState } from 'react';
import { Alert, Button, Card, Checkbox, Descriptions, Drawer, Form, Input, InputNumber, Modal, Progress, Select, Space, Statistic, Steps, Table, Tabs, Tag, Typography, Upload, message, } from 'antd';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { DownloadOutlined, EditOutlined, PlusOutlined, ReloadOutlined, SendOutlined, ThunderboltOutlined, UploadOutlined, UserAddOutlined, } from '@ant-design/icons';
import * as trainApi from '../../api/training';
import { useNavStore } from '../../stores/navStore';
import { TrainingProcessSteps } from './TrainingProcessSteps';
import './TrainingClassDivisionPage.css';
const { Text } = Typography;
const STRATEGY_LABEL = {
    by_job_level: '按工种/等级',
    by_job_level_slot: '工种等级+上下午时段',
    by_shift_team: '按班组/倒班',
    by_roster_order: '按名单顺序',
    manual: '手动组班',
};
const STATUS_COLOR = {
    draft: 'processing',
    pending: 'warning',
    rejected: 'error',
    materialized: 'success',
    superseded: 'default',
};
function clonePack(pack) {
    return JSON.parse(JSON.stringify(pack));
}
function normalizeClasses(classes) {
    return classes.map((c, idx) => ({
        ...c,
        tempId: c.tempId || `c-${idx + 1}`,
        nums: c.members?.length ?? c.nums ?? 0,
        members: (c.members ?? []).map((m) => ({ ...m })),
    }));
}
function resolveClassJobName(row) {
    if (row.jobName?.trim())
        return row.jobName.trim();
    const names = Array.from(new Set((row.members ?? [])
        .map((m) => m.jobName?.trim())
        .filter((v) => !!v)));
    return names.length === 1 ? names[0] : undefined;
}
/** 优先用已存 workTypeId；否则按工种名称在选项中反查（兼容旧草稿）。 */
function resolveClassWorkTypeId(row, flatOptions) {
    if (row.workTypeId != null && row.workTypeId > 0)
        return Number(row.workTypeId);
    const name = resolveClassJobName(row);
    if (!name)
        return undefined;
    const hit = flatOptions.find((o) => o.label === name);
    return hit?.value;
}
const CLASS_LEVEL_OPTIONS = ['初级工', '中级工', '高级工', '技师', '高级技师'].map((v) => ({
    value: v,
    label: v,
}));
function resolveClassLevelName(row) {
    if (row.levelName?.trim())
        return row.levelName.trim();
    const names = Array.from(new Set((row.members ?? [])
        .map((m) => m.levelName?.trim())
        .filter((v) => !!v)));
    return names.length === 1 ? names[0] : undefined;
}
function buildPackPatch(pack) {
    return {
        classes: pack.classes.map((c) => ({
            tempId: c.tempId,
            title: c.title,
            group: c.group,
            workTypeId: c.workTypeId != null && c.workTypeId > 0 ? Number(c.workTypeId) : undefined,
            jobName: (c.jobName ?? '').trim() || resolveClassJobName(c) || undefined,
            levelName: (c.levelName ?? '').trim() || resolveClassLevelName(c) || undefined,
            members: c.members ?? [],
        })),
        unassigned: pack.unassigned,
    };
}
export function TrainingClassDivisionPage() {
    const queryClient = useQueryClient();
    const navigateTo = useNavStore((s) => s.setSelectedKey);
    const now = new Date();
    const [step, setStep] = useState(0);
    const [keyword, setKeyword] = useState('');
    const [readyOnly, setReadyOnly] = useState(true);
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(10);
    const [selectedPlan, setSelectedPlan] = useState(null);
    const [trainInfoId, setTrainInfoId] = useState(null);
    const [trainInfoTitle, setTrainInfoTitle] = useState('');
    const [titlePrefix, setTitlePrefix] = useState('');
    const [year, setYear] = useState(now.getFullYear());
    const [month, setMonth] = useState(now.getMonth() + 1);
    const [classMax, setClassMax] = useState(50);
    const [strategy, setStrategy] = useState('by_job_level');
    const [useShiftFilter, setUseShiftFilter] = useState(true);
    const [packResult, setPackResult] = useState(null);
    const packResultRef = useRef(null);
    packResultRef.current = packResult;
    const [draftDirty, setDraftDirty] = useState(false);
    const [editClassIdx, setEditClassIdx] = useState(null);
    const [classMemberKeyword, setClassMemberKeyword] = useState('');
    const [resultTab, setResultTab] = useState('classes');
    const [poolOpen, setPoolOpen] = useState(false);
    const [poolSelectedIds, setPoolSelectedIds] = useState([]);
    const [rosterPage, setRosterPage] = useState(1);
    const [rosterPageSize, setRosterPageSize] = useState(20);
    const [poolKeywordInput, setPoolKeywordInput] = useState('');
    const [poolJobNameInput, setPoolJobNameInput] = useState('');
    const [poolKeyword, setPoolKeyword] = useState('');
    const [poolJobName, setPoolJobName] = useState('');
    const [addPersonOpen, setAddPersonOpen] = useState(false);
    const [addPersonForm] = Form.useForm();
    const [newClassTitle, setNewClassTitle] = useState('');
    const plansQuery = useQuery({
        queryKey: ['division-plans', page, pageSize, keyword, readyOnly],
        queryFn: () => trainApi.pageDivisionPlans({
            page,
            pageSize,
            keyword: keyword || undefined,
            readyOnly,
        }),
    });
    const readyQuery = useQuery({
        queryKey: ['division-ready', selectedPlan?.planId],
        queryFn: () => trainApi.getDivisionReady(selectedPlan.planId),
        enabled: !!selectedPlan?.planId,
    });
    const workTypePublicQuery = useQuery({
        queryKey: ['division-worktype-public'],
        queryFn: () => trainApi.getDemandPublicData(['worktype']),
        staleTime: 5 * 60 * 1000,
    });
    const savedPackTasksQuery = useQuery({
        queryKey: ['division-pack-tasks', 'plan-entry'],
        queryFn: async () => {
            const pageSize = 500;
            const first = await trainApi.pageDivisionPackTasks({
                page: 1,
                pageSize,
            });
            const totalPages = Math.ceil((first.total ?? 0) / pageSize);
            if (totalPages <= 1)
                return first;
            const rest = [];
            for (let p = 2; p <= totalPages; p++) {
                rest.push(trainApi.pageDivisionPackTasks({
                    page: p,
                    pageSize,
                }));
            }
            const pages = await Promise.all(rest);
            return {
                ...first,
                page: 1,
                pageSize: first.total,
                items: [
                    ...first.items,
                    ...pages.flatMap((item) => item.items),
                ],
            };
        },
        enabled: step === 0,
    });
    const rosterQuery = useQuery({
        queryKey: [
            'division-roster',
            selectedPlan?.planId,
            trainInfoId,
            poolOpen,
            poolKeyword,
            poolJobName,
        ],
        queryFn: async () => {
            const pageSize = 500;
            const baseParams = {
                planId: selectedPlan.planId,
                trainInfoId: trainInfoId ?? undefined,
                assignStatus: 0,
                keyword: poolKeyword || undefined,
                jobName: poolJobName || undefined,
            };
            const first = await trainApi.pageDivisionRoster({
                ...baseParams,
                page: 1,
                pageSize,
            });
            const totalPages = Math.ceil((first.total ?? 0) / pageSize);
            if (totalPages <= 1)
                return first;
            const rest = [];
            for (let p = 2; p <= totalPages; p++) {
                rest.push(trainApi.pageDivisionRoster({
                    ...baseParams,
                    page: p,
                    pageSize,
                }));
            }
            const pages = await Promise.all(rest);
            return {
                ...first,
                page: 1,
                pageSize: first.total,
                items: [
                    ...first.items,
                    ...pages.flatMap((item) => item.items),
                ],
            };
        },
        enabled: !!selectedPlan?.planId && (poolOpen || step === 2),
    });
    const materialsOk = !!(selectedPlan
        && (selectedPlan.rosterFileId || readyQuery.data?.rosterFileId)
        && (selectedPlan.shiftFileId || readyQuery.data?.shiftFileId));
    const missing = readyQuery.data?.missing ?? [];
    const rosterFileName = selectedPlan?.rosterFileName || readyQuery.data?.rosterFileName;
    const shiftFileName = selectedPlan?.shiftFileName || readyQuery.data?.shiftFileName;
    const editable = !!packResult && (packResult.status === 'draft' || packResult.status === 'rejected');
    const editingClass = editClassIdx != null && packResult ? packResult.classes[editClassIdx] : null;
    const classMemberItems = useMemo(() => {
        const rows = editingClass?.members ?? [];
        const kw = classMemberKeyword.trim().toLowerCase();
        if (!kw)
            return rows;
        return rows.filter((row) => (row.personName ?? '').toLowerCase().includes(kw)
            || (row.idCard ?? '').toLowerCase().includes(kw));
    }, [editingClass?.members, classMemberKeyword]);
    const savedPackTaskByPlanId = useMemo(() => {
        const priority = {
            draft: 0,
            rejected: 1,
            pending: 2,
            materialized: 3,
        };
        const map = new Map();
        for (const task of savedPackTasksQuery.data?.items ?? []) {
            if (!task.planId || task.status === 'superseded')
                continue;
            const current = map.get(task.planId);
            const currentPriority = current ? priority[current.status] ?? 99 : 99;
            const taskPriority = priority[task.status] ?? 99;
            const isNewerSameStatus = current
                && taskPriority === currentPriority
                && (task.updateTime ?? task.createTime ?? '') > (current.updateTime ?? current.createTime ?? '');
            if (!current || taskPriority < currentPriority || isNewerSameStatus) {
                map.set(task.planId, task);
            }
        }
        return map;
    }, [savedPackTasksQuery.data?.items]);
    const enterPlan = (row) => {
        if (!row.rosterFileId || !row.shiftFileId) {
            message.warning('需同时具备筛选名单与排班表');
            return;
        }
        setSelectedPlan(row);
        setTrainInfoId(null);
        setTrainInfoTitle('');
        setTitlePrefix(row.orgName || row.title);
        setPackResult(null);
        setDraftDirty(false);
        setPoolSelectedIds([]);
        setResultTab('classes');
        setStep(1);
    };
    const applyLocalPack = (next) => {
        const normalized = {
            ...next,
            classes: normalizeClasses(next.classes ?? []),
            unassigned: next.unassigned ?? [],
            classCount: next.classes?.length ?? 0,
            assignedCount: (next.classes ?? []).reduce((s, c) => s + (c.members?.length ?? c.nums ?? 0), 0),
            unassignedCount: next.unassigned?.length ?? 0,
        };
        setPackResult(normalized);
    };
    const openSavedPackTask = async (taskId) => {
        try {
            const detail = await trainApi.getDivisionPackTask(taskId);
            const plan = (plansQuery.data?.items ?? []).find((item) => item.planId === detail.planId);
            setSelectedPlan(plan ?? {
                planId: detail.planId,
                planNo: `#${detail.planId}`,
                title: `分班方案 #${detail.taskId}`,
                targetOrgId: detail.companyId ?? 0,
                status: 4,
                statusLabel: '企业已确认',
            });
            setTrainInfoId(detail.trainInfoId);
            setTrainInfoTitle('');
            setTitlePrefix(detail.classNamePrefix ?? '');
            setClassMax(detail.classMax ?? 50);
            if (detail.year)
                setYear(detail.year);
            if (detail.month)
                setMonth(detail.month);
            if (['by_job_level', 'by_job_level_slot', 'by_shift_team', 'by_roster_order'].includes(detail.strategy)) {
                setStrategy(detail.strategy);
            }
            applyLocalPack(detail);
            setDraftDirty(false);
            setEditClassIdx(null);
            setResultTab('classes');
            setStep(2);
        }
        catch (e) {
            message.error(e instanceof Error ? e.message : '打开分班方案失败');
        }
    };
    const ensureTrainInfo = async () => {
        if (trainInfoId)
            return trainInfoId;
        const res = await trainApi.ensureDivisionTrainInfo(selectedPlan.planId, {
            title: titlePrefix || undefined,
            reuseExisting: true,
        });
        setTrainInfoId(res.trainInfoId);
        setTrainInfoTitle(res.title);
        queryClient.invalidateQueries({ queryKey: ['division-plans'] });
        return res.trainInfoId;
    };
    const packMu = useMutation({
        mutationFn: async () => {
            const tid = await ensureTrainInfo();
            return trainApi.autoPackDivision({
                planId: selectedPlan.planId,
                trainInfoId: tid,
                strategy,
                classMax,
                classNamePrefix: titlePrefix ? `${titlePrefix}-` : undefined,
                submitForCompanyConfirm: false,
                useShiftFilter,
                excludeNoIdCard: false,
                year,
                month,
                autoParseRoster: true,
            });
        },
        onSuccess: (res) => {
            applyLocalPack(res);
            setTrainInfoId(res.trainInfoId);
            setDraftDirty(false);
            setStep(2);
            setResultTab('classes');
            const fallback = res.warnings?.find((w) => w.code === 'SLOT_FALLBACK_JOB_LEVEL');
            if (fallback?.message) {
                message.warning(fallback.message);
            }
            else {
                message.success(`已生成分班草稿：${res.classCount} 个班 / ${res.assignedCount} 人，请核对后提交企业审核`);
            }
            queryClient.invalidateQueries({ queryKey: ['division-roster'] });
            queryClient.invalidateQueries({ queryKey: ['division-plans'] });
            queryClient.invalidateQueries({ queryKey: ['division-pack-tasks'] });
        },
        onError: (e) => message.error(e.message || '自动分班失败'),
    });
    const manualPackMu = useMutation({
        mutationFn: async () => {
            const tid = await ensureTrainInfo();
            return trainApi.manualPackDivision({
                planId: selectedPlan.planId,
                trainInfoId: tid,
                classMax,
                classNamePrefix: titlePrefix ? `${titlePrefix}-` : undefined,
                classes: [{ title: `${titlePrefix || '培训'}-1班`, rosterIds: [] }],
            });
        },
        onSuccess: (res) => {
            applyLocalPack(res);
            setTrainInfoId(res.trainInfoId);
            setDraftDirty(false);
            setStep(2);
            setResultTab('classes');
            message.success('已创建手动分班草稿，请添加班级与学员后提交企业审核');
            queryClient.invalidateQueries({ queryKey: ['division-roster'] });
            queryClient.invalidateQueries({ queryKey: ['division-plans'] });
            queryClient.invalidateQueries({ queryKey: ['division-pack-tasks'] });
        },
        onError: (e) => message.error(e.message || '创建手动草稿失败'),
    });
    const saveMu = useMutation({
        mutationFn: () => {
            const pack = packResultRef.current;
            if (!pack)
                throw new Error('无草稿');
            return trainApi.patchDivisionPackTask(pack.taskId, buildPackPatch(pack));
        },
        onSuccess: (res) => {
            applyLocalPack(res);
            setDraftDirty(false);
            message.success('草稿已保存');
        },
        onError: (e) => message.error(e.message || '保存失败'),
    });
    const submitMu = useMutation({
        mutationFn: async () => {
            const pack = packResultRef.current;
            if (!pack)
                throw new Error('无草稿');
            if (draftDirty) {
                await trainApi.patchDivisionPackTask(pack.taskId, buildPackPatch(pack));
            }
            return trainApi.submitDivisionPackTask(pack.taskId);
        },
        onSuccess: (res) => {
            applyLocalPack(res);
            setDraftDirty(false);
            message.success('已提交企业审核，通过后将自动创建培训班级');
            queryClient.invalidateQueries({ queryKey: ['division-pack-tasks'] });
            queryClient.invalidateQueries({ queryKey: ['enterprise-pack-confirm'] });
        },
        onError: (e) => message.error(e.message || '提交失败'),
    });
    const syncPlatformUsersMu = useMutation({
        mutationFn: () => {
            const pack = packResultRef.current;
            if (!pack)
                throw new Error('无方案');
            return trainApi.syncDivisionPackPlatformUsers(pack.taskId);
        },
        onSuccess: (res) => {
            const sync = res.platformUserSync;
            message.success(`补开完成：新建 ${sync.created}，复用 ${sync.reused}，更新 ${sync.updated}，跳过 ${sync.skipped}`
                + (sync.created > 0 ? '；新建默认密码 123123' : '')
                + (sync.warnings?.length ? `；告警 ${sync.warnings.length} 条` : ''));
            if (sync.warnings?.length) {
                Modal.warning({
                    title: '部分学员未开账号',
                    content: (_jsx("div", { style: { maxHeight: 280, overflow: 'auto' }, children: sync.warnings.map((w, i) => (_jsxs("div", { children: [w.name || '-', "\uFF1A", w.reason] }, `${w.idCard}-${i}`))) })),
                });
            }
        },
        onError: (e) => message.error(e.message || '补开失败'),
    });
    const addPersonMu = useMutation({
        mutationFn: async (payload) => {
            const pack = packResultRef.current;
            if (!pack || editClassIdx == null)
                throw new Error('未选择班级');
            const tempId = pack.classes[editClassIdx]?.tempId;
            if (!tempId)
                throw new Error('班级 tempId 缺失，请先保存草稿');
            if (draftDirty) {
                await trainApi.patchDivisionPackTask(pack.taskId, buildPackPatch(pack));
            }
            return trainApi.addPersonToDivisionPackClass(pack.taskId, tempId, payload);
        },
        onSuccess: (res) => {
            applyLocalPack(res);
            setDraftDirty(false);
            setAddPersonOpen(false);
            addPersonForm.resetFields();
            message.success(res.createdRoster ? '已入库并加入班级' : '已加入班级');
            queryClient.invalidateQueries({ queryKey: ['division-roster'] });
        },
        onError: (e) => message.error(e.message || '加人失败'),
    });
    const importMembersMu = useMutation({
        mutationFn: async (file) => {
            const pack = packResultRef.current;
            if (!pack || editClassIdx == null)
                throw new Error('未选择班级');
            const tempId = pack.classes[editClassIdx]?.tempId;
            if (!tempId)
                throw new Error('班级 tempId 缺失，请先保存草稿');
            if (draftDirty) {
                await trainApi.patchDivisionPackTask(pack.taskId, buildPackPatch(pack));
            }
            return trainApi.importMembersToDivisionPackClass(pack.taskId, tempId, file);
        },
        onSuccess: (res) => {
            applyLocalPack(res.task);
            setDraftDirty(false);
            const skipN = res.skipped?.length ?? 0;
            if ((res.added ?? 0) === 0 && skipN > 0) {
                const sample = res.skipped?.slice(0, 3).map((s) => `${s.personName || ''}：${s.reason || ''}`).join('；');
                message.warning(`未导入成功（跳过 ${skipN} 人）。${sample || ''}`);
            }
            else {
                message.success(`导入成功 ${res.added} 人${skipN ? `，跳过 ${skipN} 人` : ''}`);
            }
            queryClient.invalidateQueries({ queryKey: ['division-roster'] });
        },
        onError: (e) => message.error(e.message || '导入失败'),
    });
    const updateDraft = (mutator) => {
        setPackResult((prev) => {
            if (!prev || (prev.status !== 'draft' && prev.status !== 'rejected'))
                return prev;
            const next = clonePack(prev);
            mutator(next);
            next.classes = normalizeClasses(next.classes);
            next.classCount = next.classes.length;
            next.assignedCount = next.classes.reduce((s, c) => s + (c.members?.length ?? 0), 0);
            next.unassignedCount = next.unassigned.length;
            packResultRef.current = next;
            return next;
        });
        setDraftDirty(true);
    };
    const moveMemberToUnassigned = (classIdx, rosterId) => {
        updateDraft((draft) => {
            const cls = draft.classes[classIdx];
            if (!cls?.members)
                return;
            const member = cls.members.find((m) => m.rosterId === rosterId);
            if (!member)
                return;
            cls.members = cls.members.filter((m) => m.rosterId !== rosterId);
            draft.unassigned = [
                ...draft.unassigned.filter((u) => u.rosterId !== rosterId),
                {
                    rosterId: member.rosterId,
                    personName: member.personName,
                    idCard: member.idCard,
                    workshop: member.workshop,
                    jobName: member.jobName,
                    levelName: member.levelName,
                    shiftTeam: member.shiftTeam,
                    reason: '机构调整：移出班级',
                },
            ];
        });
    };
    const moveUnassignedToClass = (rosterId, targetClassIdx) => {
        updateDraft((draft) => {
            const person = draft.unassigned.find((u) => u.rosterId === rosterId);
            if (!person || person.rosterId == null)
                return;
            const cls = draft.classes[targetClassIdx];
            if (!cls)
                return;
            if ((cls.members?.length ?? 0) >= draft.classMax) {
                message.warning(`目标班已达上限 ${draft.classMax}`);
                return;
            }
            cls.members = [
                ...(cls.members ?? []),
                {
                    rosterId: person.rosterId,
                    personName: person.personName,
                    idCard: person.idCard,
                    workshop: person.workshop,
                    jobName: person.jobName,
                    levelName: person.levelName,
                    shiftTeam: person.shiftTeam,
                },
            ];
            draft.unassigned = draft.unassigned.filter((u) => u.rosterId !== rosterId);
        });
    };
    const moveMemberBetweenClasses = (fromIdx, rosterId, toIdx) => {
        if (fromIdx === toIdx)
            return;
        updateDraft((draft) => {
            const from = draft.classes[fromIdx];
            const to = draft.classes[toIdx];
            if (!from?.members || !to)
                return;
            const member = from.members.find((m) => m.rosterId === rosterId);
            if (!member)
                return;
            if ((to.members?.length ?? 0) >= draft.classMax) {
                message.warning(`目标班已达上限 ${draft.classMax}`);
                return;
            }
            from.members = from.members.filter((m) => m.rosterId !== rosterId);
            to.members = [...(to.members ?? []), member];
        });
    };
    const pendingTotal = selectedPlan?.pendingRosterCount;
    const planColumns = [
        { title: '规划编号', dataIndex: 'planNo', width: 150 },
        { title: '标题', dataIndex: 'title', ellipsis: true },
        { title: '企业', dataIndex: 'orgName', width: 140, ellipsis: true },
        {
            title: '材料',
            width: 150,
            render: (_, row) => (_jsxs(Space, { size: 4, wrap: true, children: [_jsx(Tag, { color: row.rosterFileId ? 'success' : 'default', children: "\u540D\u5355" }), _jsx(Tag, { color: row.shiftFileId ? 'success' : 'default', children: "\u6392\u73ED" })] })),
        },
        {
            title: '待分',
            dataIndex: 'pendingRosterCount',
            width: 72,
            align: 'right',
            render: (v) => (v != null && v > 0 ? _jsx(Text, { strong: true, children: v }) : (v ?? '-')),
        },
        {
            title: '已建培训',
            dataIndex: 'linkedTrainInfoCount',
            width: 88,
            align: 'right',
            render: (v) => v ?? 0,
        },
        {
            title: '操作',
            width: 210,
            fixed: 'right',
            render: (_, row) => {
                const ok = !!(row.rosterFileId && row.shiftFileId);
                const savedTask = savedPackTaskByPlanId.get(row.planId);
                const canEditSaved = savedTask?.status === 'draft' || savedTask?.status === 'rejected';
                return (_jsxs(Space, { size: 4, wrap: true, children: [_jsx(Button, { type: "link", size: "small", disabled: !ok, onClick: (e) => { e.stopPropagation(); enterPlan(row); }, children: "\u5F00\u59CB\u5206\u73ED" }), savedTask && (_jsxs(_Fragment, { children: [_jsx(Tag, { color: STATUS_COLOR[savedTask.status] || 'default', style: { marginInlineEnd: 0 }, children: savedTask.statusLabel || savedTask.status }), _jsx(Button, { type: "link", size: "small", onClick: (e) => { e.stopPropagation(); openSavedPackTask(savedTask.taskId); }, children: canEditSaved ? '继续编辑' : '分班草稿' })] }))] }));
            },
        },
    ];
    const rosterColumns = [
        { title: '姓名', dataIndex: 'personName', width: 100 },
        { title: '身份证', dataIndex: 'idCard', width: 180 },
        { title: '班组', dataIndex: 'shiftTeam', width: 100, render: (v) => v || '-' },
        { title: '工种', dataIndex: 'jobName', width: 120, render: (v) => v || '-' },
    ];
    const classOptions = useMemo(() => (packResult?.classes ?? []).map((c, idx) => ({
        value: idx,
        label: `${c.title}（${c.members?.length ?? c.nums}人）`,
    })), [packResult]);
    const poolRosterItems = useMemo(() => {
        const rows = rosterQuery.data?.items ?? [];
        const kw = poolKeywordInput.trim().toLowerCase();
        const job = poolJobNameInput.trim().toLowerCase();
        if (!kw && !job)
            return rows;
        return rows.filter((row) => {
            const keywordMatched = !kw
                || (row.personName ?? '').toLowerCase().includes(kw)
                || (row.idCard ?? '').toLowerCase().includes(kw);
            const jobMatched = !job || (row.jobName ?? '').toLowerCase().includes(job);
            return keywordMatched && jobMatched;
        });
    }, [rosterQuery.data?.items, poolKeywordInput, poolJobNameInput]);
    const poolJobOptions = useMemo(() => {
        const names = new Set();
        for (const row of rosterQuery.data?.items ?? []) {
            const name = row.jobName?.trim();
            if (name)
                names.add(name);
        }
        return Array.from(names)
            .sort((a, b) => a.localeCompare(b, 'zh-CN'))
            .map((name) => ({ label: name, value: name }));
    }, [rosterQuery.data?.items]);
    /** 与开班准备一致：分组 Select，value = exam_work_type.id */
    const classWorkTypeGroupedOptions = useMemo(() => {
        return workTypePublicQuery.data?.worktype_list?.map((g) => ({
            label: g.title,
            options: (g.options ?? []).map((o) => ({ value: o.id, label: o.title })),
        })) ?? [];
    }, [workTypePublicQuery.data]);
    const classWorkTypeFlatOptions = useMemo(() => {
        return classWorkTypeGroupedOptions.flatMap((g) => g.options ?? []);
    }, [classWorkTypeGroupedOptions]);
    const applyPoolFilters = () => {
        setRosterPage(1);
        setPoolSelectedIds([]);
        setPoolKeyword(poolKeywordInput.trim());
        setPoolJobName(poolJobNameInput.trim());
    };
    const resetPoolFilters = () => {
        setRosterPage(1);
        setRosterPageSize(20);
        setPoolSelectedIds([]);
        setPoolKeywordInput('');
        setPoolJobNameInput('');
        setPoolKeyword('');
        setPoolJobName('');
    };
    const confirmPack = () => {
        if (!materialsOk) {
            message.warning(`材料未齐：${missing.join('、') || '筛选名单/排班表'}`);
            return;
        }
        Modal.confirm({
            title: '生成分班草稿？',
            width: 480,
            content: (_jsx("div", { style: { marginTop: 8 }, children: _jsxs("p", { style: { marginBottom: 8 }, children: ["\u7B56\u7565\u300C", STRATEGY_LABEL[strategy], "\u300D\uFF1A", strategy === 'by_job_level'
                            ? '按名单工种与等级分班；排班表仅过滤当月难以上课人员。缺工种/等级进未分入。'
                            : strategy === 'by_job_level_slot'
                                ? '按工种/等级分组后，结合排班表按每天上午/下午可上课情况拆成多个时段班（如 电工-高级-上午1班）。'
                                : '按选定规则切班。', "\u6BCF\u73ED\u4E0D\u8D85\u8FC7 ", classMax, " \u4EBA\uFF0C\u751F\u6210\u53EF\u7F16\u8F91\u8349\u7A3F\uFF0C\u4E0D\u4F1A\u7ACB\u5373\u5EFA\u73ED\u3002"] }) })),
            okText: '生成草稿',
            onOk: () => packMu.mutateAsync(),
        });
    };
    const confirmManualPack = () => {
        if (!materialsOk) {
            message.warning(`材料未齐：${missing.join('、') || '筛选名单/排班表'}`);
            return;
        }
        Modal.confirm({
            title: '创建手动分班草稿？',
            content: '将创建可编辑的班级草稿（不立即建班）。可多班，支持从待分池勾选、上传名单、手填加人；提交企业审核通过后才创建正式班级。新建草稿会作废同培训信息下未完成的旧草稿。',
            okText: '创建草稿',
            onOk: () => manualPackMu.mutateAsync(),
        });
    };
    const addEmptyClass = () => {
        const title = newClassTitle.trim() || `${titlePrefix || '培训'}-${(packResult?.classes.length ?? 0) + 1}班`;
        updateDraft((draft) => {
            draft.classes.push({
                tempId: `c-${Math.random().toString(36).slice(2, 10)}`,
                title,
                group: '手动',
                nums: 0,
                members: [],
            });
        });
        setNewClassTitle('');
        message.success(`已添加班级「${title}」，请保存草稿`);
    };
    const addFromPoolToEditingClass = () => {
        if (editClassIdx == null || !packResult)
            return;
        // 手动组班允许同一人进多个班：仅排除「当前编辑班」已有人员
        const occupiedInCurrent = new Set((packResult.classes[editClassIdx]?.members ?? []).map((m) => m.rosterId));
        const candidates = poolRosterItems.filter((r) => poolSelectedIds.includes(r.id) && !occupiedInCurrent.has(r.id));
        if (candidates.length === 0) {
            message.warning('请选择尚未在本班的待分人员（可同时属于其他班）');
            return;
        }
        const cls = packResult.classes[editClassIdx];
        const room = (packResult.classMax || 50) - (cls.members?.length ?? 0);
        if (candidates.length > room) {
            message.warning(`本班仅剩 ${room} 个名额`);
            return;
        }
        updateDraft((draft) => {
            const target = draft.classes[editClassIdx];
            if (!target)
                return;
            target.members = [
                ...(target.members ?? []),
                ...candidates.map((r) => ({
                    rosterId: r.id,
                    personName: r.personName,
                    idCard: r.idCard,
                    workshop: r.workshop,
                    jobName: r.jobName,
                    levelName: r.levelName,
                    shiftTeam: r.shiftTeam,
                    phone: r.phone,
                })),
            ];
        });
        setPoolOpen(false);
        setPoolSelectedIds([]);
        message.success(`已加入 ${candidates.length} 人，请保存草稿`);
    };
    const isManualPack = packResult?.strategy === 'manual';
    const confirmSubmit = () => {
        if (!packResult)
            return;
        Modal.confirm({
            title: '提交企业审核？',
            content: `将提交 ${packResult.classCount} 个班（${packResult.assignedCount} 人）给企业审核。审核通过后系统才会创建培训班级。`,
            okText: '提交审核',
            onOk: () => submitMu.mutateAsync(),
        });
    };
    return (_jsxs("div", { className: "training-class-division-page training-page", children: [_jsx(TrainingProcessSteps, { current: 2 }), _jsx("section", { className: "page-heading", children: _jsxs("div", { children: [_jsx(Typography.Title, { level: 4, style: { marginBottom: 4 }, children: "\u5206\u73ED" }), _jsxs(Text, { type: "secondary", children: ["\u6839\u636E\u4EBA\u5458\u540D\u5355\u4E0E\u6392\u73ED\u8868\u81EA\u52A8\u5212\u5206\u57F9\u8BAD\u73ED\uFF08\u9ED8\u8BA4\u6309\u5DE5\u79CD\u00D7\u7B49\u7EA7\u5207\u73ED\uFF0C\u6BCF\u73ED\u2264", classMax, "\uFF09"] })] }) }), _jsx(Card, { bordered: false, className: "division-steps-card", children: _jsx(Steps, { current: step, size: "small", items: [
                        { title: '选择规划' },
                        { title: '配置并分班' },
                        { title: '编辑并提交' },
                    ], onChange: (s) => {
                        if (s === 0) {
                            setStep(0);
                            return;
                        }
                        if (!selectedPlan) {
                            message.warning('请先选择规划');
                            return;
                        }
                        if (s >= 1 && !materialsOk) {
                            message.warning('需同时具备筛选名单与排班表');
                            return;
                        }
                        if (s >= 2 && !packResult) {
                            message.warning('请先生成分班草稿（自动或手动）');
                            return;
                        }
                        setStep(s);
                    } }) }), step === 0 && (_jsxs(Card, { bordered: false, children: [_jsxs("div", { className: "division-toolbar", children: [_jsx(Input.Search, { allowClear: true, placeholder: "\u641C\u7D22\u89C4\u5212\u6807\u9898 / \u7F16\u53F7 / \u4F01\u4E1A", style: { width: 280 }, onSearch: (v) => { setKeyword(v); setPage(1); } }), _jsx(Checkbox, { checked: readyOnly, onChange: (e) => { setReadyOnly(e.target.checked); setPage(1); }, children: "\u4EC5\u6750\u6599\u5DF2\u9F50" }), _jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => {
                                    plansQuery.refetch();
                                    savedPackTasksQuery.refetch();
                                }, children: "\u5237\u65B0" })] }), _jsx(Alert, { className: "division-plan-hint", type: "info", showIcon: true, message: "\u4E00\u952E\u5206\u73ED\u4F1A\u6309\u4F01\u4E1A\u6700\u65B0\u4E0A\u4F20\u7684\u7B5B\u9009\u540D\u5355\u91CD\u65B0\u89E3\u6790\u4EBA\u5458\uFF1B\u751F\u6210\u8349\u7A3F\u540E\u8BF7\u518D\u63D0\u4EA4\u4F01\u4E1A\u5BA1\u6838\u3002" }), _jsx(Table, { rowKey: "planId", loading: plansQuery.isLoading, columns: planColumns, dataSource: plansQuery.data?.items ?? [], scroll: { x: 900 }, onRow: (row) => ({
                            className: row.rosterFileId && row.shiftFileId ? 'division-row-ready' : undefined,
                            style: { cursor: row.rosterFileId && row.shiftFileId ? 'pointer' : 'default' },
                            onDoubleClick: () => enterPlan(row),
                        }), locale: { emptyText: readyOnly ? '暂无材料已齐的规划，可取消「仅材料已齐」查看全部' : '暂无规划' }, pagination: {
                            current: page,
                            pageSize,
                            total: plansQuery.data?.total ?? 0,
                            showSizeChanger: true,
                            showTotal: (t) => `共 ${t} 条`,
                            onChange: (p, ps) => {
                                if (typeof ps === 'number' && ps > 0 && ps !== pageSize) {
                                    setPageSize(ps);
                                    setPage(1);
                                }
                                else {
                                    setPage(p);
                                }
                            },
                        } })] })), step === 1 && selectedPlan && (_jsxs(Card, { bordered: false, children: [_jsxs("div", { className: "division-context", children: [_jsxs("div", { className: "division-context-main", children: [_jsxs("p", { className: "division-context-title", children: [selectedPlan.planNo, " \u00B7 ", selectedPlan.title] }), _jsxs("div", { className: "division-context-meta", children: ["\u4F01\u4E1A ", selectedPlan.orgName || '-', pendingTotal != null ? ` · 待分池 ${pendingTotal} 人` : '', trainInfoId ? ` · 培训信息 #${trainInfoId}` : ''] }), _jsxs(Space, { size: 4, style: { marginTop: 6 }, wrap: true, children: [_jsxs(Tag, { color: selectedPlan.rosterFileId || readyQuery.data?.rosterFileId ? 'success' : 'error', children: ["\u540D\u5355", rosterFileName ? ` · ${rosterFileName}` : ''] }), _jsxs(Tag, { color: selectedPlan.shiftFileId || readyQuery.data?.shiftFileId ? 'success' : 'error', children: ["\u6392\u73ED", shiftFileName ? ` · ${shiftFileName}` : ''] })] })] }), _jsxs(Space, { wrap: true, children: [_jsx(Button, { size: "small", icon: _jsx(DownloadOutlined, {}), disabled: !selectedPlan.rosterFileId, onClick: () => trainApi.downloadThreeYearPlanFile(selectedPlan.planId, 'enterprise-roster'), children: "\u4E0B\u8F7D\u540D\u5355" }), _jsx(Button, { size: "small", icon: _jsx(DownloadOutlined, {}), disabled: !selectedPlan.shiftFileId, onClick: () => trainApi.downloadThreeYearPlanFile(selectedPlan.planId, 'enterprise-shift'), children: "\u4E0B\u8F7D\u6392\u73ED" }), _jsx(Button, { size: "small", onClick: () => setStep(0), children: "\u91CD\u9009\u89C4\u5212" })] })] }), !materialsOk && (_jsx(Alert, { type: "warning", showIcon: true, style: { marginBottom: 16 }, message: `材料未齐：${missing.join('、') || '筛选名单/排班表'}，请先在企业审核中回传。` })), _jsxs(Descriptions, { size: "small", column: { xs: 1, sm: 2, md: 3 }, style: { marginBottom: 16 }, children: [_jsx(Descriptions.Item, { label: "\u73ED\u7EA7\u540D\u524D\u7F00", children: _jsx(Input, { value: titlePrefix, onChange: (e) => setTitlePrefix(e.target.value), placeholder: "\u5982\uFF1A\u6765\u94DD", style: { maxWidth: 220 } }) }), _jsx(Descriptions.Item, { label: "\u57F9\u8BAD\u4FE1\u606F", children: trainInfoId
                                    ? _jsxs(Text, { type: "success", children: ["#", trainInfoId, " ", trainInfoTitle || '', "\uFF08\u5206\u73ED\u65F6\u81EA\u52A8\u590D\u7528\uFF09"] })
                                    : _jsx(Text, { type: "secondary", children: "\u70B9\u51FB\u5206\u73ED\u65F6\u81EA\u52A8\u751F\u6210/\u590D\u7528" }) }), _jsx(Descriptions.Item, { label: "\u8BF4\u660E", children: _jsx(Text, { type: "secondary", children: "\u5148\u51FA\u8349\u7A3F\uFF0C\u4F01\u4E1A\u901A\u8FC7\u540E\u518D\u5EFA\u73ED" }) })] }), _jsxs(Form, { layout: "vertical", children: [_jsxs("div", { className: "division-config-grid", children: [_jsx(Form.Item, { label: "\u57F9\u8BAD/\u6392\u73ED\u5E74", required: true, children: _jsx(InputNumber, { style: { width: '100%' }, min: 2020, max: 2100, value: year, onChange: (v) => setYear(v || now.getFullYear()) }) }), _jsx(Form.Item, { label: "\u6392\u73ED\u6708", required: true, children: _jsx(InputNumber, { style: { width: '100%' }, min: 1, max: 12, value: month, onChange: (v) => setMonth(v || 1) }) }), _jsx(Form.Item, { label: "\u6BCF\u73ED\u4E0A\u9650", required: true, children: _jsx(InputNumber, { style: { width: '100%' }, min: 1, max: 200, value: classMax, onChange: (v) => setClassMax(v || 50) }) }), _jsx(Form.Item, { label: "\u5206\u73ED\u7B56\u7565", children: _jsx(Select, { value: strategy, onChange: setStrategy, options: [
                                                { value: 'by_job_level', label: '按工种/等级切班（推荐）' },
                                                { value: 'by_shift_team', label: '按班组/倒班切班' },
                                                { value: 'by_job_level_slot', label: '工种/等级 + 上下午时段（高级）' },
                                                { value: 'by_roster_order', label: '按名单顺序切班' },
                                            ] }) })] }), _jsx(Checkbox, { checked: useShiftFilter, onChange: (e) => setUseShiftFilter(e.target.checked), children: "\u8FC7\u6EE4\u5F53\u6708\u65E0\u53EF\u4E0A\u8BFE\u65E5\u4EBA\u5458\uFF08\u5168\u591C/\u5168\u4F11\u7B49\uFF09" }), _jsx("div", { style: { marginTop: 8 }, children: _jsx(Text, { type: "secondary", children: "\u9ED8\u8BA4\u6309\u5DE5\u79CD\u00D7\u7B49\u7EA7\u5206\u73ED\uFF1B\u6392\u73ED\u53EA\u505A\u8FC7\u6EE4\u3002\u540D\u5355\u5E26\u5E74\u4EFD\u65F6\uFF0C\u6240\u9009\u5E74\u65E0\u4EBA\u5C06\u76F4\u63A5\u62A5\u9519\u3002" }) })] }), _jsxs("div", { className: "division-actions", children: [_jsx(Button, { type: "primary", size: "large", icon: _jsx(ThunderboltOutlined, {}), loading: packMu.isPending, disabled: !materialsOk, onClick: confirmPack, children: "\u4E00\u952E\u81EA\u52A8\u5206\u73ED" }), _jsx(Button, { size: "large", icon: _jsx(UserAddOutlined, {}), loading: manualPackMu.isPending, disabled: !materialsOk, onClick: confirmManualPack, children: "\u624B\u52A8\u521B\u5EFA\u73ED\u7EA7" }), _jsx(Text, { type: "secondary", children: "\u81EA\u52A8/\u624B\u52A8\u5747\u751F\u6210\u8349\u7A3F\uFF0C\u4F01\u4E1A\u901A\u8FC7\u540E\u624D\u5EFA\u73ED\uFF1B\u65B0\u5EFA\u8349\u7A3F\u4F1A\u4F5C\u5E9F\u540C\u57F9\u8BAD\u4E0B\u65E7\u8349\u7A3F" })] })] })), step === 2 && selectedPlan && packResult && (_jsxs(Card, { bordered: false, children: [_jsxs("div", { className: "division-context", style: { marginBottom: 16 }, children: [_jsxs("div", { className: "division-context-main", children: [_jsxs("p", { className: "division-context-title", children: [selectedPlan.planNo, " \u00B7 ", selectedPlan.title] }), _jsxs("div", { className: "division-context-meta", children: ["\u65B9\u6848 #", packResult.taskId, ' · ', "\u7B56\u7565 ", STRATEGY_LABEL[packResult.strategy] || packResult.strategy, ' · ', "\u6BCF\u73ED\u4E0A\u9650 ", packResult.classMax, trainInfoId ? ` · 培训信息 #${trainInfoId}` : ''] })] }), _jsxs(Space, { wrap: true, children: [_jsx(Tag, { color: STATUS_COLOR[packResult.status] || 'default', children: packResult.statusLabel || packResult.status }), draftDirty && _jsx(Tag, { color: "orange", children: "\u6709\u672A\u4FDD\u5B58\u4FEE\u6539" })] })] }), packResult.status === 'rejected' && packResult.rejectReason && (_jsx(Alert, { type: "error", showIcon: true, style: { marginBottom: 16 }, message: "\u4F01\u4E1A\u5DF2\u9A73\u56DE", description: packResult.rejectReason })), packResult.status === 'pending' && (_jsx(Alert, { type: "info", showIcon: true, style: { marginBottom: 16 }, message: "\u5DF2\u63D0\u4EA4\u4F01\u4E1A\u5BA1\u6838\uFF0C\u901A\u8FC7\u540E\u5C06\u81EA\u52A8\u521B\u5EFA\u57F9\u8BAD\u73ED\u7EA7\u3002\u5BA1\u6838\u524D\u4E0D\u53EF\u518D\u7F16\u8F91\u3002" })), packResult.status === 'materialized' && (_jsx(Alert, { type: "success", showIcon: true, style: { marginBottom: 16 }, message: "\u4F01\u4E1A\u5DF2\u901A\u8FC7\uFF0C\u57F9\u8BAD\u73ED\u7EA7\u5DF2\u521B\u5EFA", description: packResult.materialized
                            ? `已建 ${packResult.materialized.classCount} 班 / ${packResult.materialized.peopleCount} 人。若学员无法登录 App，可点击「补开 App 账号」。`
                            : '若学员无法登录 App，可点击「补开 App 账号」。' })), editable && (_jsx(Alert, { type: "warning", showIcon: true, style: { marginBottom: 16 }, message: "\u5F53\u524D\u4E3A\u8349\u7A3F\uFF1A\u53EF\u6539\u73ED\u7EA7\u540D\u79F0\u3001\u8C03\u6574\u5B66\u5458\u5F52\u5C5E\uFF1B\u4FDD\u5B58\u540E\u63D0\u4EA4\u4F01\u4E1A\u5BA1\u6838\uFF0C\u901A\u8FC7\u540E\u624D\u771F\u6B63\u5EFA\u73ED\u3002" })), _jsx(Card, { size: "small", className: "division-result-stats", bordered: false, style: { background: '#f8fafc' }, children: _jsxs(Space, { size: 48, wrap: true, children: [_jsx(Statistic, { title: "\u73ED\u7EA7\u6570", value: packResult.classCount, suffix: "\u4E2A" }), _jsx(Statistic, { title: "\u5DF2\u5206\u914D", value: packResult.assignedCount, suffix: "\u4EBA" }), _jsx(Statistic, { title: "\u672A\u5206\u5165", value: packResult.unassignedCount, suffix: "\u4EBA", valueStyle: packResult.unassignedCount > 0 ? { color: '#d46b08' } : undefined })] }) }), editable && isManualPack && (_jsxs(Space, { style: { marginBottom: 12, marginTop: 12 }, wrap: true, children: [_jsx(Input, { style: { width: 220 }, placeholder: "\u65B0\u73ED\u7EA7\u540D\u79F0", value: newClassTitle, onChange: (e) => setNewClassTitle(e.target.value) }), _jsx(Button, { icon: _jsx(PlusOutlined, {}), onClick: addEmptyClass, children: "\u65B0\u5EFA\u73ED\u7EA7" }), _jsx(Text, { type: "secondary", children: "\u624B\u52A8\u7EC4\u73ED\uFF1A\u540C\u4E00\u4EBA\u53EF\u8FDB\u5165\u591A\u4E2A\u73ED\uFF1B\u7F16\u8F91\u5B66\u5458\u652F\u6301\u5F85\u5206\u6C60\u52FE\u9009\u3001\u4E0A\u4F20\u540D\u5355\u3001\u624B\u586B\u52A0\u4EBA" })] })), _jsx(Tabs, { activeKey: resultTab, onChange: setResultTab, items: [
                            {
                                key: 'classes',
                                label: `班级草稿 (${packResult.classes?.length ?? 0})`,
                                children: (_jsx(Table, { rowKey: (r) => r.tempId || r.title, size: "small", pagination: false, dataSource: packResult.classes, columns: [
                                        {
                                            title: '班级名称',
                                            dataIndex: 'title',
                                            render: (title, _row, idx) => (editable ? (_jsx(Input, { value: title, onChange: (e) => {
                                                    updateDraft((d) => {
                                                        if (d.classes[idx])
                                                            d.classes[idx].title = e.target.value;
                                                    });
                                                } })) : title),
                                        },
                                        { title: '分组', dataIndex: 'group', width: 120 },
                                        {
                                            title: '工种',
                                            dataIndex: 'workTypeId',
                                            width: 160,
                                            render: (_v, row, idx) => {
                                                const workTypeId = resolveClassWorkTypeId(row, classWorkTypeFlatOptions);
                                                const label = (workTypeId != null
                                                    ? classWorkTypeFlatOptions.find((o) => o.value === workTypeId)?.label
                                                    : undefined) || resolveClassJobName(row);
                                                if (!editable) {
                                                    return label ? _jsx(Tag, { children: label }) : _jsx(Text, { type: "secondary", children: "-" });
                                                }
                                                return (_jsx(Select, { allowClear: true, showSearch: true, placeholder: "\u9009\u62E9\u5DE5\u79CD", style: { width: '100%' }, value: workTypeId, options: classWorkTypeGroupedOptions, optionFilterProp: "label", onChange: (id) => {
                                                        const nextId = id != null ? Number(id) : undefined;
                                                        const nextLabel = nextId != null
                                                            ? classWorkTypeFlatOptions.find((o) => o.value === nextId)?.label
                                                            : undefined;
                                                        updateDraft((d) => {
                                                            if (!d.classes[idx])
                                                                return;
                                                            d.classes[idx].workTypeId = nextId;
                                                            d.classes[idx].jobName = nextLabel || undefined;
                                                        });
                                                    } }));
                                            },
                                        },
                                        {
                                            title: '等级',
                                            dataIndex: 'levelName',
                                            width: 130,
                                            render: (_v, row, idx) => {
                                                const current = resolveClassLevelName(row);
                                                if (!editable) {
                                                    return current ? _jsx(Tag, { children: current }) : _jsx(Text, { type: "secondary", children: "-" });
                                                }
                                                return (_jsx(Select, { mode: "tags", maxCount: 1, allowClear: true, showSearch: true, placeholder: "\u9009\u62E9\u6216\u8F93\u5165\u7B49\u7EA7", style: { width: '100%' }, value: current ? [current] : [], options: CLASS_LEVEL_OPTIONS, optionFilterProp: "label", tokenSeparators: [], onChange: (vals) => {
                                                        const next = (vals?.[0] ?? '').trim();
                                                        updateDraft((d) => {
                                                            if (!d.classes[idx])
                                                                return;
                                                            d.classes[idx].levelName = next || undefined;
                                                        });
                                                    } }));
                                            },
                                        },
                                        {
                                            title: '人数',
                                            width: 160,
                                            render: (_, row) => {
                                                const nums = row.members?.length ?? row.nums ?? 0;
                                                return (_jsxs("div", { className: "division-fill", children: [_jsx("span", { children: nums }), _jsx(Progress, { percent: Math.min(100, Math.round((nums / (packResult.classMax || 50)) * 100)), size: "small", showInfo: false }), _jsxs(Text, { type: "secondary", style: { fontSize: 12 }, children: ["/", packResult.classMax] })] }));
                                            },
                                        },
                                        {
                                            title: '操作',
                                            width: 100,
                                            render: (_v, _row, idx) => (_jsx(Button, { type: "link", size: "small", icon: _jsx(EditOutlined, {}), onClick: () => {
                                                    setClassMemberKeyword('');
                                                    setEditClassIdx(idx);
                                                }, children: editable ? '编辑学员' : '查看学员' })),
                                        },
                                    ] })),
                            },
                            {
                                key: 'unassigned',
                                label: `未分入 (${packResult.unassignedCount})`,
                                children: (packResult.unassigned?.length ?? 0) === 0 ? (_jsx(Alert, { type: "success", showIcon: true, message: "\u5168\u90E8\u53EF\u5206\u4EBA\u5458\u5747\u5DF2\u5165\u73ED\u8349\u7A3F" })) : (_jsx(Table, { rowKey: (r) => `${r.rosterId}-${r.reason}`, size: "small", pagination: { pageSize: 10, showSizeChanger: false, showTotal: (t) => `共 ${t} 人` }, dataSource: packResult.unassigned, columns: [
                                        { title: '姓名', dataIndex: 'personName', width: 90 },
                                        { title: '单位', dataIndex: 'workshop', width: 120, ellipsis: true, render: (v) => v || '-' },
                                        { title: '工种', dataIndex: 'jobName', width: 110, ellipsis: true, render: (v) => v || '-' },
                                        { title: '等级', dataIndex: 'levelName', width: 90, render: (v) => v || '-' },
                                        { title: '身份证', dataIndex: 'idCard', width: 170 },
                                        { title: '原因', dataIndex: 'reason' },
                                        ...(editable ? [{
                                                title: '操作',
                                                width: 180,
                                                render: (_, row) => (_jsx(Select, { placeholder: "\u5206\u5165\u73ED\u7EA7", style: { width: 160 }, options: classOptions, onChange: (idx) => {
                                                        if (row.rosterId != null)
                                                            moveUnassignedToClass(row.rosterId, idx);
                                                    } })),
                                            }] : []),
                                    ] })),
                            },
                            ...(packResult.warnings?.length
                                ? [{
                                        key: 'warnings',
                                        label: `警告 (${packResult.warnings.length})`,
                                        children: (_jsx(Alert, { type: "warning", showIcon: true, message: `共 ${packResult.warnings.length} 条提示`, description: (_jsx("ul", { style: { margin: '8px 0 0', paddingLeft: 18 }, children: packResult.warnings.slice(0, 20).map((w, i) => (_jsx("li", { children: w.message }, i))) })) })),
                                    }]
                                : []),
                        ] }), _jsxs(Space, { style: { marginTop: 20 }, wrap: true, children: [_jsx(Button, { onClick: () => setStep(1), children: "\u8FD4\u56DE\u914D\u7F6E" }), editable && (_jsxs(_Fragment, { children: [_jsx(Button, { loading: saveMu.isPending, disabled: !draftDirty, onClick: () => saveMu.mutate(), children: "\u4FDD\u5B58\u8349\u7A3F" }), _jsx(Button, { type: "primary", icon: _jsx(SendOutlined, {}), loading: submitMu.isPending, onClick: confirmSubmit, children: "\u63D0\u4EA4\u4F01\u4E1A\u5BA1\u6838" })] })), packResult.status === 'materialized' && (_jsxs(_Fragment, { children: [_jsx(Button, { type: "primary", onClick: () => navigateTo('train_people-list'), children: "\u53BB\u73ED\u7EA7\u4EBA\u5458" }), _jsx(Button, { loading: syncPlatformUsersMu.isPending, onClick: () => syncPlatformUsersMu.mutate(), children: "\u8865\u5F00 App \u8D26\u53F7" }), _jsx(Button, { onClick: () => navigateTo('company_plan_review'), children: "\u53BB\u4F01\u4E1A\u5206\u73ED\u786E\u8BA4" })] })), _jsx(Button, { onClick: () => {
                                    setStep(0);
                                    setPackResult(null);
                                    setSelectedPlan(null);
                                    setTrainInfoId(null);
                                    setDraftDirty(false);
                                }, children: "\u5904\u7406\u4E0B\u4E00\u89C4\u5212" })] })] })), _jsx(Drawer, { title: editingClass ? `${editable ? '编辑' : '查看'}：${editingClass.title}` : '班级学员', width: 960, open: editClassIdx != null, onClose: () => {
                    setClassMemberKeyword('');
                    setEditClassIdx(null);
                }, children: editingClass && (_jsxs(_Fragment, { children: [editable && isManualPack && (_jsxs(Space, { style: { marginBottom: 12 }, wrap: true, children: [_jsx(Button, { icon: _jsx(UserAddOutlined, {}), onClick: () => {
                                        setPoolSelectedIds([]);
                                        setRosterPage(1);
                                        setRosterPageSize(20);
                                        setPoolKeywordInput('');
                                        setPoolJobNameInput('');
                                        setPoolKeyword('');
                                        setPoolJobName('');
                                        setPoolOpen(true);
                                    }, children: "\u4ECE\u5F85\u5206\u6C60\u52FE\u9009" }), _jsx(Upload, { accept: ".xlsx,.xls", showUploadList: false, beforeUpload: (file) => {
                                        importMembersMu.mutate(file);
                                        return false;
                                    }, children: _jsx(Button, { icon: _jsx(UploadOutlined, {}), loading: importMembersMu.isPending, children: "\u4E0A\u4F20\u540D\u5355" }) }), _jsx(Button, { onClick: async () => {
                                        try {
                                            const res = await trainApi.downloadDivisionMemberImportTemplate();
                                            const blob = res.data;
                                            const url = URL.createObjectURL(blob);
                                            const a = document.createElement('a');
                                            a.href = url;
                                            a.download = 'class-member-import-template.xlsx';
                                            a.click();
                                            URL.revokeObjectURL(url);
                                        }
                                        catch (e) {
                                            message.error(e.message || '下载模板失败');
                                        }
                                    }, children: "\u4E0B\u8F7D\u5BFC\u5165\u6A21\u677F" }), _jsx(Button, { type: "primary", ghost: true, icon: _jsx(PlusOutlined, {}), onClick: () => {
                                        addPersonForm.resetFields();
                                        setAddPersonOpen(true);
                                    }, children: "\u624B\u586B\u52A0\u4EBA" })] })), _jsx(Input.Search, { allowClear: true, style: { width: 280, marginBottom: 12 }, placeholder: "\u59D3\u540D / \u8EAB\u4EFD\u8BC1\u53F7", value: classMemberKeyword, onChange: (e) => setClassMemberKeyword(e.target.value) }), _jsx(Table, { rowKey: "rosterId", size: "small", pagination: false, dataSource: classMemberItems, locale: { emptyText: '该班暂无学员' }, columns: [
                                { title: '姓名', dataIndex: 'personName', width: 90 },
                                { title: '单位', dataIndex: 'workshop', width: 120, ellipsis: true, render: (v) => v || '-' },
                                { title: '工种', dataIndex: 'jobName', width: 110, ellipsis: true, render: (v) => v || '-' },
                                { title: '等级', dataIndex: 'levelName', width: 90, render: (v) => v || '-' },
                                { title: '班组', dataIndex: 'shiftTeam', width: 90, render: (v) => v || '-' },
                                { title: '身份证', dataIndex: 'idCard', width: 170 },
                                ...(editable && editClassIdx != null ? [{
                                        title: '调整',
                                        width: 220,
                                        render: (_, row) => (_jsxs(Space, { children: [_jsx(Select, { placeholder: "\u8C03\u5230\u5176\u4ED6\u73ED", style: { width: 140 }, options: classOptions.filter((o) => o.value !== editClassIdx), onChange: (toIdx) => moveMemberBetweenClasses(editClassIdx, row.rosterId, toIdx) }), _jsx(Button, { type: "link", danger: true, size: "small", onClick: () => moveMemberToUnassigned(editClassIdx, row.rosterId), children: "\u79FB\u51FA" })] })),
                                    }] : []),
                            ] })] })) }), _jsxs(Modal, { title: "\u4ECE\u5F85\u5206\u6C60\u52FE\u9009\u52A0\u4EBA", open: poolOpen, onCancel: () => setPoolOpen(false), onOk: addFromPoolToEditingClass, okText: "\u52A0\u5165\u672C\u73ED", width: 800, destroyOnClose: true, children: [_jsxs(Space, { style: { marginBottom: 12 }, wrap: true, children: [_jsx(Input.Search, { allowClear: true, style: { width: 260 }, placeholder: "\u59D3\u540D / \u8EAB\u4EFD\u8BC1\u53F7", value: poolKeywordInput, onChange: (e) => {
                                    setRosterPage(1);
                                    setPoolKeywordInput(e.target.value);
                                }, onSearch: applyPoolFilters }), _jsx(Select, { allowClear: true, showSearch: true, style: { width: 220 }, placeholder: "\u5DE5\u79CD", value: poolJobNameInput, options: poolJobOptions, optionFilterProp: "label", onChange: (value) => {
                                    setRosterPage(1);
                                    setPoolJobNameInput(value ?? '');
                                } }), _jsx(Button, { onClick: applyPoolFilters, children: "\u7B5B\u9009" }), _jsx(Button, { onClick: resetPoolFilters, children: "\u91CD\u7F6E" }), poolSelectedIds.length > 0 && (_jsxs(Text, { type: "secondary", children: ["\u5DF2\u9009 ", poolSelectedIds.length, " \u4EBA"] }))] }), _jsx(Table, { rowKey: "id", size: "small", loading: rosterQuery.isLoading, columns: rosterColumns, dataSource: poolRosterItems, rowSelection: {
                            selectedRowKeys: poolSelectedIds,
                            onChange: (keys) => setPoolSelectedIds(keys),
                            preserveSelectedRowKeys: true,
                        }, pagination: {
                            current: rosterPage,
                            pageSize: rosterPageSize,
                            total: poolRosterItems.length,
                            showTotal: (t) => `共 ${t} 人`,
                            showSizeChanger: true,
                            pageSizeOptions: [20, 50, 100, 200, 500],
                            onChange: (p, ps) => {
                                setRosterPage(p);
                                setRosterPageSize(ps);
                            },
                        } })] }), _jsx(Modal, { title: "\u624B\u586B\u52A0\u4EBA", open: addPersonOpen, onCancel: () => setAddPersonOpen(false), onOk: () => addPersonForm.submit(), confirmLoading: addPersonMu.isPending, destroyOnClose: true, children: _jsxs(Form, { form: addPersonForm, layout: "vertical", onFinish: (values) => addPersonMu.mutate(values), children: [_jsx(Form.Item, { name: "personName", label: "\u59D3\u540D", rules: [{ required: true, message: '请输入姓名' }], children: _jsx(Input, { placeholder: "\u5FC5\u586B" }) }), _jsx(Form.Item, { name: "idCard", label: "\u8EAB\u4EFD\u8BC1", extra: "\u5F3A\u70C8\u5EFA\u8BAE\u586B\u5199\uFF1B\u65E0\u8BC1\u65F6\u540C\u540D\u591A\u4EBA\u987B\u8865\u8BC1", children: _jsx(Input, { placeholder: "\u5EFA\u8BAE\u586B\u5199" }) }), _jsx(Form.Item, { name: "phone", label: "\u624B\u673A", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "jobName", label: "\u5DE5\u79CD", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "levelName", label: "\u7B49\u7EA7", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "workshop", label: "\u8F66\u95F4/\u5355\u4F4D", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "shiftTeam", label: "\u73ED\u7EC4", children: _jsx(Input, {}) })] }) })] }));
}

import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useCallback, useEffect, useMemo, useState } from 'react';
import { Alert, Button, Card, Empty, Form, Input, InputNumber, Modal, Segmented, Select, Space, Table, Tabs, Tag, Typography, message, } from 'antd';
import { PlusOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../api/training';
import { TrainingProcessSteps } from './TrainingProcessSteps';
import { SubsidyClassWorkbench } from './subsidy/SubsidyClassWorkbench';
import './TrainingSubsidyDeclarePage.css';
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
    return CLASS_LEVEL_LABELS[n] ?? `${n}级`;
}
const PHASE_OPTS = [
    { value: 'draft', label: '草稿' },
    { value: 'checking', label: '核验中' },
    { value: 'ready', label: '材料就绪' },
    { value: 'submitted', label: '已提交' },
    { value: 'rejected', label: '已退回' },
    { value: 'approved', label: '已通过' },
    { value: 'granting', label: '拨付中' },
    { value: 'granted', label: '已拨付' },
    { value: 'closed', label: '已关闭' },
];
const PHASE_COLOR = {
    draft: 'default',
    checking: 'processing',
    ready: 'cyan',
    submitted: 'blue',
    rejected: 'error',
    approved: 'success',
    granting: 'gold',
    granted: 'green',
    closed: 'default',
};
const REGION_OPTS = [
    { value: '来宾市', label: '来宾市', code: '来宾' },
    { value: '玉林市', label: '玉林市', code: '玉林' },
    { value: '河池市南丹县', label: '河池市南丹县', code: '河池南丹' },
];
const CLOSED_PHASES = new Set(['approved', 'granting', 'granted', 'closed']);
function phaseLabel(v) {
    return PHASE_OPTS.find((p) => p.value === v)?.label || v || '—';
}
function money(v) {
    if (v == null)
        return '—';
    return `¥${Number(v).toLocaleString('zh-CN', { maximumFractionDigits: 2 })}`;
}
function classSubsidyStatus(s) {
    if (!s)
        return 'none';
    if (CLOSED_PHASES.has(s.phase || ''))
        return 'done';
    return 'open';
}
/** 拉全部分页补贴单，供班级状态/看板（避免单页截断） */
async function fetchAllSubsidyItems() {
    const pageSize = 200;
    const maxPages = 500; // 最多约 10 万条
    const items = [];
    let page = 1;
    let total = Number.POSITIVE_INFINITY;
    while (items.length < total && page <= maxPages) {
        const res = await trainApi.pageSubsidies({ page, pageSize });
        const batch = res.items ?? [];
        items.push(...batch);
        total = typeof res.total === 'number' ? res.total : items.length;
        if (batch.length === 0)
            break;
        page += 1;
    }
    const truncated = items.length < total;
    return { items, total, truncated };
}
/** 状态筛选开启时拉全部分页班级再本地过滤，避免服务端分页 + 前端滤后 total 错乱 */
async function fetchAllInfoClasses(params) {
    const pageSize = 200;
    const maxPages = 500;
    const list = [];
    let page = 1;
    let total = Number.POSITIVE_INFINITY;
    while (list.length < total && page <= maxPages) {
        const res = await trainApi.pageInfoClassList({
            page,
            pageSize,
            keyword: params.keyword || undefined,
            certFilter: params.certFilter,
        });
        const batch = res.list ?? [];
        list.push(...batch);
        total = typeof res.total === 'number' ? res.total : list.length;
        if (batch.length === 0)
            break;
        page += 1;
    }
    const truncated = list.length < total;
    return { list, total: truncated ? list.length : total, truncated };
}
/** 运营流程 · 申报补贴：班级列表 → 本班材料工作台 */
export function TrainingSubsidyDeclarePage() {
    const qc = useQueryClient();
    const [mainTab, setMainTab] = useState('classes');
    const [phase, setPhase] = useState();
    const [regionFilter, setRegionFilter] = useState();
    const [yearFilter, setYearFilter] = useState();
    const [classFilter, setClassFilter] = useState();
    const [keyword, setKeyword] = useState('');
    const [keywordInput, setKeywordInput] = useState('');
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(20);
    const [classPage, setClassPage] = useState(1);
    const [classPageSize, setClassPageSize] = useState(20);
    const [classKeyword, setClassKeyword] = useState('');
    const [classKwInput, setClassKwInput] = useState('');
    /** 默认只看已获证班（有证书回填） */
    const [certFilter, setCertFilter] = useState('certified');
    const [subsidyStatusFilter, setSubsidyStatusFilter] = useState('all');
    const [createOpen, setCreateOpen] = useState(false);
    const [detail, setDetail] = useState(null);
    const [inWorkbench, setInWorkbench] = useState(false);
    const [workBusy, setWorkBusy] = useState(false);
    const [enteringClassId, setEnteringClassId] = useState(null);
    const [form] = Form.useForm();
    const watchedRegion = Form.useWatch('regionName', form);
    const watchedYear = Form.useWatch('policyYear', form);
    const watchedProfileId = Form.useWatch('profileId', form);
    const watchedClassId = Form.useWatch('classId', form);
    const listQ = useQuery({
        queryKey: ['subsidies', phase, regionFilter, yearFilter, classFilter, keyword, page, pageSize],
        queryFn: () => trainApi.pageSubsidies({
            page,
            pageSize,
            phase,
            regionCode: regionFilter,
            policyYear: yearFilter,
            classId: classFilter,
            keyword: keyword || undefined,
        }),
        enabled: !inWorkbench && mainTab === 'ledger',
    });
    const statsQ = useQuery({
        queryKey: ['subsidies-stats'],
        queryFn: async () => {
            const res = await fetchAllSubsidyItems();
            return {
                items: res.items,
                total: res.total,
                page: 1,
                pageSize: res.items.length,
                truncated: res.truncated,
            };
        },
        enabled: !inWorkbench,
    });
    const classListQ = useQuery({
        queryKey: [
            'subsidy-class-list',
            classPage,
            classPageSize,
            classKeyword,
            certFilter,
            subsidyStatusFilter,
        ],
        queryFn: async () => {
            if (subsidyStatusFilter === 'all') {
                return trainApi.pageInfoClassList({
                    page: classPage,
                    pageSize: classPageSize,
                    keyword: classKeyword || undefined,
                    certFilter,
                });
            }
            // 状态筛：全量班级 + 本地分页（依赖 subsidies-stats 映射）
            return fetchAllInfoClasses({
                keyword: classKeyword || undefined,
                certFilter,
            });
        },
        enabled: !inWorkbench,
    });
    const classesPickQ = useQuery({
        queryKey: ['info-classes-brief-all'],
        queryFn: () => fetchAllInfoClasses({ certFilter: 'all' }),
        enabled: createOpen,
    });
    const profilesQ = useQuery({
        queryKey: ['subsidy-profiles-active'],
        queryFn: () => trainApi.listSubsidyPackProfiles({ status: 'active', trainMode: 'skill_upgrade' }),
    });
    const invalidate = () => {
        void qc.invalidateQueries({ queryKey: ['subsidies'] });
        void qc.invalidateQueries({ queryKey: ['subsidies-stats'] });
        void qc.invalidateQueries({ queryKey: ['subsidy-class-list'] });
    };
    const openDetail = async (row) => {
        try {
            const fresh = await trainApi.getSubsidy(row.id);
            setDetail(fresh);
            return fresh;
        }
        catch (e) {
            message.error(e.message || '加载补贴单失败，请稍后重试');
            throw e;
        }
    };
    const enterWorkbench = async (row) => {
        const fresh = await openDetail(row);
        setInWorkbench(true);
        return fresh;
    };
    const exitWorkbench = () => {
        setInWorkbench(false);
        setDetail(null);
        invalidate();
    };
    const openClassWorkbench = async (classId) => {
        setWorkBusy(true);
        setEnteringClassId(classId);
        try {
            const row = await trainApi.ensureClassSubsidy(classId);
            setClassFilter(classId);
            invalidate();
            await enterWorkbench(row);
            message.success(`已进入材料工作台：${row.className || `班级 #${classId}`}`);
        }
        catch (e) {
            const msg = e.message || '打开本班补贴失败';
            message.error(msg);
            setCreateOpen(true);
            form.setFieldsValue({ classId, trainMode: 'skill_upgrade' });
            message.info('请手工选择申报地市与政策年后创建');
        }
        finally {
            setWorkBusy(false);
            setEnteringClassId(null);
        }
    };
    const openDetailById = async (id) => {
        try {
            const fresh = await trainApi.getSubsidy(id);
            if (fresh.classId != null)
                setClassFilter(fresh.classId);
            await enterWorkbench(fresh);
        }
        catch (e) {
            message.error(e.message || '打开补贴单失败');
        }
    };
    const applySubsidyFocus = useCallback(() => {
        const focus = trainApi.consumeSubsidyDeclareFocus();
        if (!focus)
            return;
        if (focus.classId != null) {
            setClassFilter(focus.classId);
            setClassKwInput(String(focus.classId));
        }
        if (focus.subsidyId != null) {
            void openDetailById(focus.subsidyId);
            return;
        }
        if (focus.openCreate && focus.classId != null) {
            setCreateOpen(true);
            form.setFieldsValue({ classId: focus.classId, trainMode: 'skill_upgrade' });
            return;
        }
        if (focus.classId != null) {
            void openClassWorkbench(focus.classId);
        }
        // openClassWorkbench / openDetailById 稳定引用不足，刻意不列入 deps
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [form]);
    useEffect(() => {
        applySubsidyFocus();
        const onFocusEvent = () => applySubsidyFocus();
        window.addEventListener(trainApi.SUBSIDY_DECLARE_FOCUS_EVENT, onFocusEvent);
        return () => window.removeEventListener(trainApi.SUBSIDY_DECLARE_FOCUS_EVENT, onFocusEvent);
    }, [applySubsidyFocus]);
    const createMu = useMutation({
        mutationFn: trainApi.createSubsidy,
        onSuccess: (row) => {
            message.success('已创建补贴单');
            setCreateOpen(false);
            form.resetFields();
            if (row.classId != null)
                setClassFilter(row.classId);
            invalidate();
            void enterWorkbench(row).catch(() => undefined);
        },
        onError: (e) => message.error(e.message || '创建失败'),
    });
    const classRows = useMemo(() => classesPickQ.data?.list ?? [], [classesPickQ.data]);
    const listClassRows = useMemo(() => classListQ.data?.list ?? [], [classListQ.data]);
    const classMap = useMemo(() => {
        const map = new Map();
        for (const c of classRows)
            map.set(String(c.id), c);
        for (const c of listClassRows)
            map.set(String(c.id), c);
        return map;
    }, [classRows, listClassRows]);
    const classOptions = useMemo(() => {
        return classRows.map((c) => {
            const bits = [
                c.title || '班级',
                c.company_name || null,
                c.date_end ? `结业 ${c.date_end}` : null,
                c.nums != null ? `${c.nums}人` : null,
            ].filter(Boolean);
            return { value: c.id, label: `${bits.join(' · ')} (#${c.id})` };
        });
    }, [classRows]);
    const subsidyByClass = useMemo(() => {
        const map = new Map();
        for (const s of statsQ.data?.items ?? []) {
            if (s.classId == null)
                continue;
            const key = String(s.classId);
            const prev = map.get(key);
            if (!prev) {
                map.set(key, s);
                continue;
            }
            if (CLOSED_PHASES.has(prev.phase || '') && !CLOSED_PHASES.has(s.phase || '')) {
                map.set(key, s);
            }
        }
        return map;
    }, [statsQ.data]);
    const displayClassRows = useMemo(() => {
        const rows = listClassRows;
        if (subsidyStatusFilter === 'all')
            return rows;
        const filtered = rows.filter((c) => {
            const st = classSubsidyStatus(subsidyByClass.get(String(c.id)));
            return st === subsidyStatusFilter;
        });
        const start = (classPage - 1) * classPageSize;
        return filtered.slice(start, start + classPageSize);
    }, [listClassRows, subsidyByClass, subsidyStatusFilter, classPage, classPageSize]);
    const displayClassTotal = useMemo(() => {
        if (subsidyStatusFilter === 'all') {
            return classListQ.data?.total ?? 0;
        }
        return listClassRows.filter((c) => {
            const st = classSubsidyStatus(subsidyByClass.get(String(c.id)));
            return st === subsidyStatusFilter;
        }).length;
    }, [subsidyStatusFilter, classListQ.data?.total, listClassRows, subsidyByClass]);
    const profiles = profilesQ.data || [];
    const matchedProfile = useMemo(() => {
        if (watchedProfileId) {
            return profiles.find((p) => String(p.id) === String(watchedProfileId));
        }
        if (!watchedRegion)
            return undefined;
        const region = REGION_OPTS.find((r) => r.value === watchedRegion);
        const year = watchedYear ?? new Date().getFullYear();
        return profiles.find((p) => p.status === 'active' &&
            (p.regionName === watchedRegion ||
                p.regionCode === region?.code ||
                (region && p.regionName.includes(region.code))) &&
            p.policyYear === year);
    }, [profiles, watchedProfileId, watchedRegion, watchedYear]);
    const phaseCounts = useMemo(() => {
        const rows = statsQ.data?.items ?? [];
        const map = { __all: rows.length };
        for (const r of rows) {
            const k = r.phase || 'draft';
            map[k] = (map[k] || 0) + 1;
        }
        return map;
    }, [statsQ.data]);
    const stats = useMemo(() => {
        const rows = statsQ.data?.items ?? [];
        return {
            total: rows.length,
            draft: rows.filter((r) => r.phase === 'draft' || r.phase === 'checking').length,
            ready: rows.filter((r) => r.phase === 'ready' || r.phase === 'submitted').length,
            done: rows.filter((r) => ['approved', 'granting', 'granted'].includes(r.phase || '')).length,
        };
    }, [statsQ.data]);
    const columns = [
        {
            title: '班级',
            dataIndex: 'className',
            width: 260,
            ellipsis: true,
            render: (v, r) => (_jsx(Button, { type: "link", style: { padding: 0, height: 'auto' }, onClick: () => void enterWorkbench(r), children: v || `班级 #${r.classId}` || '—' })),
        },
        {
            title: '地市',
            dataIndex: 'regionName',
            width: 80,
            render: (v, r) => v || r.regionCode || '—',
        },
        { title: '政策年', dataIndex: 'policyYear', width: 80, align: 'center' },
        {
            title: '人数 / 金额',
            width: 140,
            render: (_, r) => (_jsxs("span", { children: [r.studentCount ?? '—', "\u4EBA \u00B7 ", money(r.subsidyAmount)] })),
        },
        {
            title: '阶段',
            dataIndex: 'phase',
            width: 100,
            render: (v) => _jsx(Tag, { color: PHASE_COLOR[v] || 'default', children: phaseLabel(v) }),
        },
        {
            title: '配方',
            width: 160,
            ellipsis: true,
            render: (_, r) => {
                const p = profiles.find((x) => String(x.id) === String(r.profileId));
                const title = p?.title || (r.profileId ? `#${r.profileId}` : '—');
                return (_jsxs(Space, { size: 4, children: [_jsx("span", { children: title }), r.profileLocked ? _jsx(Tag, { color: "orange", children: "\u5DF2\u9501" }) : null] }));
            },
        },
        {
            title: '材料',
            width: 90,
            render: (_, r) => r.materialPackagePath ? _jsx(Tag, { color: "success", children: "\u5DF2\u751F\u6210" }) : _jsx(Tag, { children: "\u672A\u751F\u6210" }),
        },
        {
            title: '操作',
            width: 88,
            fixed: 'right',
            render: (_, r) => (_jsx(Button, { type: "link", size: "small", onClick: () => void enterWorkbench(r), children: "\u529E\u7406" })),
        },
    ];
    const classColumns = [
        {
            title: '班级',
            dataIndex: 'title',
            ellipsis: true,
            render: (v, r) => (_jsx(Button, { type: "link", style: { padding: 0, height: 'auto', whiteSpace: 'normal', textAlign: 'left' }, loading: enteringClassId != null && String(enteringClassId) === String(r.id), onClick: () => void openClassWorkbench(r.id), children: v || `班级 #${r.id}` })),
        },
        { title: '企业', dataIndex: 'company_name', ellipsis: true, width: 160, render: (v) => v || '—' },
        { title: '工种', dataIndex: 'work_type_title', ellipsis: true, width: 140, render: (v) => v || '—' },
        {
            title: '等级',
            dataIndex: 'work_type_level',
            width: 120,
            ellipsis: true,
            render: (v) => formatClassLevel(v),
        },
        {
            title: '获证/人数',
            key: 'certified_nums',
            width: 96,
            align: 'center',
            render: (_, r) => {
                const total = r.nums ?? 0;
                const certified = r.certified_nums ?? 0;
                return `${certified}/${total}`;
            },
        },
        {
            title: '培训期',
            width: 180,
            render: (_, r) => r.date_start || r.date_end ? `${r.date_start || '—'} ~ ${r.date_end || '—'}` : '—',
        },
        {
            title: '补贴进度',
            width: 120,
            render: (_, r) => {
                const s = subsidyByClass.get(String(r.id));
                const st = classSubsidyStatus(s);
                if (st === 'none')
                    return _jsx(Tag, { children: "\u672A\u5EFA\u5355" });
                if (st === 'done')
                    return _jsx(Tag, { color: "success", children: phaseLabel(s?.phase) });
                return _jsx(Tag, { color: "processing", children: phaseLabel(s?.phase) });
            },
        },
        {
            title: '材料包',
            width: 88,
            render: (_, r) => {
                const s = subsidyByClass.get(String(r.id));
                return s?.materialPackagePath ? _jsx(Tag, { color: "success", children: "\u5DF2\u751F\u6210" }) : _jsx(Tag, { children: "\u672A\u751F\u6210" });
            },
        },
        {
            title: '操作',
            width: 120,
            fixed: 'right',
            render: (_, r) => (_jsx(Button, { type: "link", size: "small", loading: enteringClassId != null && String(enteringClassId) === String(r.id), onClick: () => void openClassWorkbench(r.id), children: "\u8FDB\u5165\u6750\u6599\u5DE5\u4F5C\u53F0" })),
        },
    ];
    const onClassChange = (classId) => {
        const c = classMap.get(String(classId));
        if (!c)
            return;
        if (c.nums != null)
            form.setFieldValue('studentCount', c.nums);
        if (c.date_end) {
            const y = Number(String(c.date_end).slice(0, 4));
            if (y >= 2020 && y <= 2035)
                form.setFieldValue('policyYear', y);
        }
        void trainApi
            .getClassSubsidy(classId)
            .then((existing) => {
            if (existing) {
                message.warning(`该班已有进行中补贴单 #${existing.id}，将打开已有单据`);
                setCreateOpen(false);
                form.resetFields();
                setClassFilter(classId);
                void enterWorkbench(existing).catch(() => undefined);
            }
        })
            .catch((e) => {
            message.error(e.message || '查询本班补贴单失败');
        });
    };
    const applySearch = () => {
        setPage(1);
        setKeyword(keywordInput.trim());
    };
    const applyClassSearch = () => {
        setClassPage(1);
        setClassKeyword(classKwInput.trim());
    };
    if (inWorkbench && detail) {
        return (_jsx(SubsidyClassWorkbench, { subsidy: detail, onBack: exitWorkbench, onSubsidyChange: setDetail }));
    }
    return (_jsxs("div", { className: "training-page training-subsidy-declare-page", children: [_jsxs("section", { className: "page-heading", children: [_jsxs("div", { children: [_jsx(Typography.Title, { level: 4, style: { marginBottom: 4 }, children: "\u7533\u62A5\u8865\u8D34" }), _jsx(Typography.Text, { type: "secondary", children: "\u5148\u9009\u73ED\u7EA7\u8FDB\u5165\u6750\u6599\u5DE5\u4F5C\u53F0\uFF1A\u6C47\u96C6\u672C\u73ED\u8DDF\u73ED/\u7ED3\u4E1A\u6750\u6599 \u2192 \u8865\u9F50\u76D6\u7AE0\u4EF6 \u2192 \u5BFC\u51FA\u7533\u62A5\u5305" })] }), _jsxs(Space, { children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => {
                                    invalidate();
                                    void classListQ.refetch();
                                }, children: "\u5237\u65B0" }), _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: () => setCreateOpen(true), children: "\u65B0\u5EFA\u8865\u8D34\u5355" })] })] }), _jsx(TrainingProcessSteps, { current: 8 }), _jsx(Alert, { type: "info", showIcon: true, style: { marginBottom: 12 }, message: "\u6BCF\u4E2A\u73ED\u6750\u6599\u4E0D\u540C\uFF0C\u8BF7\u9010\u73ED\u529E\u7406\u3002\u70B9\u300C\u8FDB\u5165\u6750\u6599\u5DE5\u4F5C\u53F0\u300D\u81EA\u52A8\u6253\u5F00/\u521B\u5EFA\u672C\u73ED\u8865\u8D34\u5355\u3002" }), statsQ.data?.truncated || classListQ.data?.truncated ? (_jsx(Alert, { type: "warning", showIcon: true, style: { marginBottom: 12 }, message: "\u6570\u636E\u91CF\u8FC7\u5927\uFF0C\u770B\u677F/\u72B6\u6001\u7B5B\u9009\u53EF\u80FD\u672A\u52A0\u8F7D\u5168\u90E8\u8BB0\u5F55", description: statsQ.data?.truncated
                    ? `补贴单已加载 ${statsQ.data.items.length} / ${statsQ.data.total}，进度标签可能不全。`
                    : '班级列表在状态筛选下未全部加载，请缩小筛选条件后重试。' })) : null, _jsxs("div", { className: "subsidy-stat-row", children: [_jsxs("div", { className: "subsidy-stat-card", children: [_jsx("div", { className: "label", children: "\u73ED\u7EA7\u603B\u6570" }), _jsx("div", { className: "value", children: classListQ.data?.total ?? '—' })] }), _jsxs("div", { className: "subsidy-stat-card", children: [_jsx("div", { className: "label", children: "\u5DF2\u5EFA\u8865\u8D34\u5355" }), _jsx("div", { className: "value", children: stats.total })] }), _jsxs("div", { className: "subsidy-stat-card", children: [_jsx("div", { className: "label", children: "\u8349\u7A3F / \u6838\u9A8C" }), _jsx("div", { className: "value", children: stats.draft })] }), _jsxs("div", { className: "subsidy-stat-card", children: [_jsx("div", { className: "label", children: "\u5C31\u7EEA / \u5DF2\u63D0\u4EA4" }), _jsx("div", { className: "value", children: stats.ready })] })] }), _jsx(Tabs, { activeKey: mainTab, onChange: (k) => setMainTab(k), items: [
                    {
                        key: 'classes',
                        label: '班级办理',
                        children: (_jsxs(Card, { size: "small", title: certFilter === 'certified'
                                ? '已获证班级'
                                : certFilter === 'uncertified'
                                    ? '未获证班级'
                                    : '全部班级', children: [_jsxs("div", { className: "subsidy-filter-bar", children: [_jsx(Segmented, { value: certFilter, onChange: (v) => {
                                                setCertFilter(v);
                                                setClassPage(1);
                                            }, options: [
                                                { value: 'certified', label: '已获证班' },
                                                { value: 'uncertified', label: '未获证班' },
                                                { value: 'all', label: '全部' },
                                            ] }), _jsx(Input, { allowClear: true, prefix: _jsx(SearchOutlined, {}), placeholder: "\u641C\u7D22\u73ED\u7EA7 / \u4F01\u4E1A / \u5DE5\u79CD", style: { width: 280 }, value: classKwInput, onChange: (e) => setClassKwInput(e.target.value), onPressEnter: applyClassSearch }), _jsx(Button, { type: "primary", onClick: applyClassSearch, children: "\u67E5\u8BE2" }), _jsx(Select, { style: { width: 160 }, value: subsidyStatusFilter, onChange: (v) => {
                                                setSubsidyStatusFilter(v);
                                                setClassPage(1);
                                            }, options: [
                                                { value: 'all', label: '全部状态' },
                                                { value: 'none', label: '未建单' },
                                                { value: 'open', label: '办理中' },
                                                { value: 'done', label: '已就绪/提交' },
                                            ] })] }), _jsx(Table, { rowKey: "id", size: "middle", loading: classListQ.isLoading || statsQ.isLoading || workBusy, columns: classColumns, dataSource: displayClassRows, scroll: { x: 1100 }, locale: {
                                        emptyText: (_jsx(Empty, { description: certFilter === 'certified'
                                                ? '暂无已获证班级，请先在毕业典礼完成证书回填'
                                                : certFilter === 'uncertified'
                                                    ? '暂无未获证班级'
                                                    : '暂无班级，请先在开班申报中建班' })),
                                    }, onRow: (r) => ({
                                        onDoubleClick: () => void openClassWorkbench(r.id),
                                        style: { cursor: 'pointer' },
                                    }), pagination: {
                                        current: classPage,
                                        pageSize: classPageSize,
                                        total: displayClassTotal,
                                        showSizeChanger: true,
                                        showTotal: (t) => `共 ${t} 个班级`,
                                        onChange: (p, ps) => {
                                            if (typeof ps === 'number' && ps > 0 && ps !== classPageSize) {
                                                setClassPageSize(ps);
                                                setClassPage(1);
                                            }
                                            else {
                                                setClassPage(p);
                                            }
                                        },
                                    } })] })),
                    },
                    {
                        key: 'ledger',
                        label: '补贴台账',
                        children: (_jsxs(Card, { size: "small", title: "\u8DE8\u73ED\u8865\u8D34\u53F0\u8D26", children: [_jsxs("div", { className: "subsidy-phase-chips", children: [_jsxs(Tag, { className: "subsidy-phase-chip", color: !phase ? 'blue' : 'default', onClick: () => {
                                                setPhase(undefined);
                                                setPage(1);
                                            }, children: ["\u5168\u90E8 ", phaseCounts.__all ?? 0] }), PHASE_OPTS.map((p) => (_jsxs(Tag, { className: "subsidy-phase-chip", color: phase === p.value ? PHASE_COLOR[p.value] || 'blue' : 'default', onClick: () => {
                                                setPhase(p.value);
                                                setPage(1);
                                            }, children: [p.label, " ", phaseCounts[p.value] ?? 0] }, p.value)))] }), _jsxs("div", { className: "subsidy-filter-bar", children: [_jsx(Select, { allowClear: true, placeholder: "\u7533\u62A5\u5730\u5E02", style: { width: 160 }, options: REGION_OPTS.map((r) => ({ value: r.code, label: r.label })), value: regionFilter, onChange: (v) => {
                                                setRegionFilter(v);
                                                setPage(1);
                                            } }), _jsx(Select, { allowClear: true, placeholder: "\u653F\u7B56\u5E74", style: { width: 120 }, options: [2024, 2025, 2026].map((y) => ({ value: y, label: String(y) })), value: yearFilter, onChange: (v) => {
                                                setYearFilter(v);
                                                setPage(1);
                                            } }), _jsx(Input, { allowClear: true, prefix: _jsx(SearchOutlined, {}), placeholder: "\u641C\u7D22\u73ED\u540D / \u5730\u5E02 / \u5907\u6CE8", style: { width: 220 }, value: keywordInput, onChange: (e) => setKeywordInput(e.target.value), onPressEnter: applySearch }), _jsx(Button, { onClick: applySearch, children: "\u67E5\u8BE2" })] }), _jsx(Table, { rowKey: "id", size: "small", loading: listQ.isLoading, columns: columns, dataSource: listQ.data?.items ?? [], scroll: { x: 960 }, locale: { emptyText: _jsx(Empty, { description: "\u6682\u65E0\u8865\u8D34\u5355\uFF0C\u8BF7\u4ECE\u300C\u73ED\u7EA7\u529E\u7406\u300D\u8FDB\u5165" }) }, pagination: {
                                        current: page,
                                        pageSize,
                                        total: listQ.data?.total ?? 0,
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
                                    } })] })),
                    },
                ] }), _jsx(Modal, { title: "\u65B0\u5EFA\u8865\u8D34\u5355", open: createOpen, onCancel: () => setCreateOpen(false), onOk: () => form.submit(), confirmLoading: createMu.isPending, destroyOnClose: true, width: 560, children: _jsxs(Form, { form: form, layout: "vertical", initialValues: { trainMode: 'skill_upgrade' }, onFinish: (v) => {
                        const region = REGION_OPTS.find((r) => r.value === v.regionName);
                        createMu.mutate({
                            ...v,
                            regionCode: region?.code || v.regionName,
                        });
                    }, children: [_jsx(Form.Item, { name: "classId", label: "\u73ED\u7EA7", rules: [{ required: true, message: '请选择班级' }], children: _jsx(Select, { showSearch: true, optionFilterProp: "label", loading: classesPickQ.isLoading, options: classOptions, placeholder: "\u9009\u62E9\u73ED\u7EA7\uFF08\u4E00\u73ED\u4E00\u5355\uFF09", notFoundContent: classesPickQ.isError
                                    ? '班级列表加载失败'
                                    : classesPickQ.isLoading
                                        ? '加载中…'
                                        : '暂无班级，请先在开班申报中建班', onChange: onClassChange }) }), _jsx(Form.Item, { name: "trainMode", label: "\u57F9\u8BAD\u6A21\u5F0F", rules: [{ required: true }], children: _jsx(Select, { options: [{ value: 'skill_upgrade', label: '岗位技能提升' }] }) }), _jsx(Form.Item, { name: "regionName", label: "\u7533\u62A5\u5730\u5E02", extra: "\u53EF\u7A7A\uFF1A\u4ECE\u57F9\u8BAD\u5355\u57CE\u5E02\u5E26\u51FA\uFF1B\u7A7A\u4E14\u65E0\u6CD5\u89E3\u6790\u65F6\u4F1A\u521B\u5EFA\u5931\u8D25", children: _jsx(Select, { allowClear: true, options: REGION_OPTS, placeholder: "\u6765\u5BBE / \u7389\u6797 / \u5357\u4E39" }) }), _jsx(Form.Item, { name: "policyYear", label: "\u653F\u7B56\u5E74", extra: "\u53EF\u7A7A\uFF1A\u6309\u73ED\u7EA7\u7ED3\u4E1A\u65E5\u63A8\u5BFC", children: _jsx(InputNumber, { style: { width: '100%' }, min: 2020, max: 2035, placeholder: "\u5982 2024" }) }), _jsx(Form.Item, { name: "profileId", label: "\u6307\u5B9A\u914D\u65B9", extra: "\u53EF\u7A7A\uFF1A\u6309 \u5730\u5E02\u00D7\u6A21\u5F0F\u00D7\u653F\u7B56\u5E74 \u81EA\u52A8\u5339\u914D active \u914D\u65B9", children: _jsx(Select, { allowClear: true, options: profiles.map((p) => ({
                                    value: p.id,
                                    label: `${p.title}（${p.regionCode} · ${p.policyYear} · ${p.profileVersion}）`,
                                })) }) }), matchedProfile && (_jsxs("div", { className: "profile-hint", children: ["\u5C06\u4F7F\u7528\u914D\u65B9\uFF1A", _jsx("b", { children: matchedProfile.title }), "\uFF08", matchedProfile.regionName, " \u00B7 ", matchedProfile.policyYear, " \u00B7 ", matchedProfile.profileVersion, "\uFF09 \u00B7 ", matchedProfile.slots?.length ?? 0, " \u4E2A\u69FD\u4F4D"] })), !matchedProfile && watchedClassId && (_jsx(Alert, { type: "warning", showIcon: true, style: { marginBottom: 12 }, message: "\u5F53\u524D\u6761\u4EF6\u53EF\u80FD\u5339\u914D\u4E0D\u5230 active \u914D\u65B9\uFF0C\u5EFA\u8BAE\u6307\u5B9A\u914D\u65B9\u6216\u6838\u5BF9\u5730\u5E02/\u653F\u7B56\u5E74" })), _jsxs("div", { style: { display: 'flex', gap: 12 }, children: [_jsx(Form.Item, { name: "studentCount", label: "\u4EBA\u6570", style: { flex: 1 }, extra: "\u53EF\u7A7A\uFF1A\u4F18\u5148\u7528\u73ED\u7EA7\u8BA1\u5212\u4EBA\u6570\uFF1B\u63D0\u4EA4\u65F6\u82E5\u4ECD\u7A7A\u5219\u6309\u82B1\u540D\u518C\u5728\u518C\u5B66\u5458\u6570", children: _jsx(InputNumber, { style: { width: '100%' }, min: 0 }) }), _jsx(Form.Item, { name: "subsidyAmount", label: "\u8865\u8D34\u91D1\u989D", style: { flex: 1 }, children: _jsx(InputNumber, { style: { width: '100%' }, min: 0 }) })] }), _jsx(Form.Item, { name: "remark", label: "\u5907\u6CE8", children: _jsx(Input.TextArea, { rows: 2, placeholder: "\u53EF\u9009" }) })] }) })] }));
}

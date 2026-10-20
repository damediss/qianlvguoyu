import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
/**
 * 操作技能试题生成管理页面 —— 方案 v1.1 §T5
 * 两页签：细目表导入（13 列表格上传/校验报告/源表下载/行内"生成"）+ 生成工作台
 * （发起区固定 5 题/点 + 批次总览 + 草稿审核三件套/评分表编辑 + 入库闸门 UI）。
 */
import React, { useMemo, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { Alert, Button, Drawer, Input, InputNumber, message, Modal, Popconfirm, Popover, Progress, Radio, Select, Space, Table, Tabs, Tag, Tooltip, Upload, } from 'antd';
import { UploadOutlined } from '@ant-design/icons';
import { treeWorkTypes } from '../../api/exam';
import { deleteSkillBlueprint, downloadSkillBlueprintSource, editSkillDraft, exportSkillDrafts, getSkillGenProgress, getSkillPublishPreview, importSkillBlueprint, listSkillBlueprints, listSkillDrafts, listSkillGenBatches, listSkillModelConfigs, pauseSkillGenBatch, publishSkillDrafts, resumeSkillGenBatch, retrySkillGenBatch, reviewSkillDrafts, submitSkillGenBatch, } from './skillGenApi';
import { LEVEL_NAME, STATUS_TAG, parseCheckReport } from './workbench/constants';
import { usePaneActive, usePaneActiveRef } from '../../layouts/TabKeepAlive';
import './workbench/GenWorkbench.css';
const LEVEL_OPTIONS = [
    { value: 5, label: '五级/初级工' },
    { value: 4, label: '四级/中级工' },
    { value: 3, label: '三级/高级工' },
    { value: 2, label: '二级/技师' },
    { value: 1, label: '一级/高级技师' },
];
/** 每考评点固定题量（与后端 SkillBlueprintApplicationService.PER_POINT_QUOTA 一致） */
const PER_POINT_QUOTA = 5;
/** 等级 → 题库总量下限（后端 MIN_QUESTIONS 同口径） */
const MIN_QUESTIONS = { 1: 30, 2: 30, 3: 50, 4: 50, 5: 50 };
const parseScoring = (raw) => {
    if (!raw)
        return [];
    try {
        const arr = JSON.parse(raw);
        return Array.isArray(arr)
            ? arr.map((o) => ({
                item: o?.item ?? '', score: String(o?.score ?? ''), scoreDesc: o?.scoreDesc ?? '',
                maxDeduct: String(o?.maxDeduct ?? ''), step: String(o?.step ?? ''),
            }))
            : [];
    }
    catch {
        return [];
    }
};
const scoringSum = (rows) => rows.reduce((acc, r) => acc + (Number(String(r.score).replace('分', '')) || 0), 0);
const REVIEW_TAG = {
    draft: { color: 'default', text: '待审' },
    approved: { color: 'success', text: '已通过' },
    rejected: { color: 'error', text: '已驳回' },
};
/**
 * "失败 N" 悬停说明（2026-09-10 用户确认）：
 * 该计数是块级生成失败的累计次数（只增不减，重试成功也不回退），是否已自愈取决于是否仍有缺口。
 */
const failedHint = (b) => {
    const gap = Math.max(0, (b.totalPoints ?? 0) - (b.donePoints ?? 0));
    return gap > 0
        ? `生成失败累计 ${b.failedPoints} 次，仍有 ${gap} 个考评点未生成——可点行内「补跑缺口」补跑`
        : `生成失败累计 ${b.failedPoints} 次（均已自动补跑成功，当前无缺口）`;
};
const SkillQuestionGenPage = () => {
    const [tab, setTab] = useState('import');
    const qc = useQueryClient();
    const paneActive = usePaneActive(); // 标签页不可见时暂停轮询/进度 tick（§3.6）
    const paneActiveRef = usePaneActiveRef(paneActive); // 长生命周期 setInterval 闭包读 ref
    // ---------------- 页签1：细目表导入 ----------------
    const [workTypeId, setWorkTypeId] = useState();
    const [level, setLevel] = useState(2);
    const [importing, setImporting] = useState(false);
    const [importPercent, setImportPercent] = useState(0);
    const importTimerRef = React.useRef(null);
    const [downloadingBpId, setDownloadingBpId] = useState();
    const [presetBpId, setPresetBpId] = useState();
    const wtTree = useQuery({ queryKey: ['skillgen-worktypes'], queryFn: treeWorkTypes });
    // 全量清单：仅供发起区/筛选下拉使用（不受导入页分页影响）
    const bpQuery = useQuery({
        queryKey: ['skillgen-blueprints'],
        queryFn: () => listSkillBlueprints({ page: 1, pageSize: 100 }),
    });
    // 细目表导入列表：服务端分页
    const [bpListPage, setBpListPage] = useState(1);
    const [bpListPageSize, setBpListPageSize] = useState(20);
    const bpListQuery = useQuery({
        queryKey: ['skillgen-blueprints', 'list', bpListPage, bpListPageSize],
        queryFn: () => listSkillBlueprints({ page: bpListPage, pageSize: bpListPageSize }),
    });
    const workTypeOptions = useMemo(() => {
        const rows = [];
        const walk = (list, prefix) => {
            for (const w of list ?? []) {
                const path = prefix ? `${prefix}/${w.title}` : w.title;
                rows.push({ id: w.id, title: w.title, path });
                if (w.children?.length)
                    walk(w.children, path);
            }
        };
        walk(wtTree.data ?? [], '');
        const titleCount = new Map();
        for (const r of rows)
            titleCount.set(r.title, (titleCount.get(r.title) ?? 0) + 1);
        return rows.map((r) => ({
            value: r.id,
            label: (titleCount.get(r.title) ?? 0) > 1 ? r.path : r.title,
        }));
    }, [wtTree.data]);
    const doImport = async (file) => {
        if (!workTypeId || !level) {
            message.warning('请先选择工种与等级');
            return false;
        }
        setImporting(true);
        setImportPercent(0);
        if (importTimerRef.current !== null)
            window.clearInterval(importTimerRef.current);
        importTimerRef.current = window.setInterval(() => {
            // 标签不可见时计时继续、跳过 setState（§3.6 乙组口径：不中断任务）
            if (paneActiveRef.current) {
                setImportPercent((p) => (p >= 95 ? p : Math.min(95, p + Math.max(1, Math.round((95 - p) / 10)))));
            }
        }, 300);
        try {
            const bp = await importSkillBlueprint(file, workTypeId, level);
            const errs = parseCheckReport(bp.checkReport);
            if (errs.length) {
                Modal.warning({
                    title: `导入完成，有 ${errs.length} 条提示`,
                    width: 560,
                    content: (_jsxs("div", { children: [_jsx("div", { style: { marginBottom: 8, color: '#888' }, children: "\u4EE5\u4E0B\u5185\u5BB9\u8BF7\u5BF9\u7167\u6E90\u6587\u4EF6\u6838\u5BF9\uFF0C\u4FEE\u6B63\u540E\u53EF\u91CD\u65B0\u5BFC\u5165\uFF1A" }), _jsx("ul", { style: { margin: 0, paddingLeft: 18 }, children: errs.map((e, i) => _jsx("li", { children: e }, i)) })] })),
                });
            }
            else {
                message.success(`导入成功：考评点 ${bp.pointCount} 个，X:Y:Z=${bp.xyzRatio ?? '-'}`);
            }
            qc.invalidateQueries({ queryKey: ['skillgen-blueprints'] });
        }
        catch {
            // 错误提示由拦截器统一处理
        }
        finally {
            if (importTimerRef.current !== null)
                window.clearInterval(importTimerRef.current);
            setImportPercent(100);
            setImporting(false);
            window.setTimeout(() => setImportPercent(0), 600);
        }
        return false;
    };
    const toWorkbench = (bp) => {
        setPresetBpId(bp.id);
        setTab('workbench');
    };
    return (_jsx("div", { style: { padding: 16 }, children: _jsx(Tabs, { activeKey: tab, onChange: setTab, items: [
                {
                    key: 'import',
                    label: '细目表导入',
                    children: (_jsxs("div", { children: [_jsxs(Space, { wrap: true, style: { marginBottom: 12 }, children: [_jsx(Select, { showSearch: true, optionFilterProp: "label", style: { width: 280 }, placeholder: "\u9009\u62E9\u5DE5\u79CD", options: workTypeOptions, value: workTypeId, onChange: (v) => setWorkTypeId(v) }), _jsx(Select, { style: { width: 160 }, placeholder: "\u9009\u62E9\u7B49\u7EA7", options: LEVEL_OPTIONS, value: level, onChange: (v) => setLevel(v) }), _jsx(Upload, { accept: ".xlsx,.xls", showUploadList: false, beforeUpload: (file) => {
                                            void doImport(file);
                                            return false;
                                        }, disabled: importing, children: _jsx(Button, { type: "primary", icon: _jsx(UploadOutlined, {}), loading: importing, children: importing ? '解析中…' : '上传操作技能细目表' }) }), importing && (_jsxs("span", { style: { width: 240 }, children: [_jsx(Progress, { percent: importPercent, size: "small", status: "active" }), _jsx("span", { style: { color: '#888' }, children: "\u6B63\u5728\u89E3\u6790\u6807\u51C6\u6587\u6863\uFF0C\u8BF7\u52FF\u5173\u95ED\u9875\u9762\u2026" })] }))] }), _jsx(Table, { rowKey: "id", size: "small", loading: bpListQuery.isLoading, dataSource: bpListQuery.data?.items ?? [], pagination: {
                                    current: bpListPage,
                                    pageSize: bpListPageSize,
                                    total: bpListQuery.data?.total,
                                    showSizeChanger: true,
                                    pageSizeOptions: [20, 30, 40, 50],
                                    showTotal: (t) => `共 ${t} 条`,
                                    onChange: (p, s) => {
                                        setBpListPage(s !== bpListPageSize ? 1 : p);
                                        setBpListPageSize(s);
                                    },
                                }, columns: [
                                    { title: '细目表批次', dataIndex: 'title', ellipsis: true },
                                    {
                                        title: '工种/等级', key: 'wl', width: 200,
                                        render: (_, b) => `${workTypeOptions.find((w) => w.value === b.workTypeId)?.label ?? b.workTypeId} · ${LEVEL_NAME[b.level] ?? b.level}`,
                                    },
                                    { title: '考评点', dataIndex: 'pointCount', width: 80 },
                                    {
                                        title: '预计产出', key: 'plan', width: 110,
                                        render: (_, b) => `${b.pointCount * PER_POINT_QUOTA} 题`,
                                    },
                                    { title: 'X:Y:Z', dataIndex: 'xyzRatio', width: 100 },
                                    {
                                        title: '校验', key: 'check', width: 110,
                                        render: (_, b) => {
                                            const errs = parseCheckReport(b.checkReport);
                                            if (!errs.length)
                                                return _jsx(Tag, { color: "success", children: "\u901A\u8FC7" });
                                            return (_jsx(Popover, { title: "\u6821\u9A8C\u63D0\u793A", content: (_jsx("ul", { style: { margin: 0, paddingLeft: 18, maxWidth: 420 }, children: errs.map((e, i) => _jsx("li", { children: e }, i)) })), children: _jsx(Tag, { color: b.weightCheck === 1 ? 'warning' : 'error', children: b.weightCheck === 1 ? `${errs.length} 条提示` : `未通过（${errs.length}）` }) }));
                                        },
                                    },
                                    {
                                        title: '导入时间', key: 'time', width: 140,
                                        render: (_, b) => b.createdAt ? dayjs(b.createdAt).format('MM-DD HH:mm') : '-',
                                    },
                                    {
                                        title: '操作', key: 'op', width: 260,
                                        render: (_, b) => (_jsxs(Space, { size: 4, children: [_jsx(Button, { size: "small", type: "primary", onClick: () => toWorkbench(b), children: "\u751F\u6210" }), _jsx(Button, { size: "small", loading: downloadingBpId === b.id, onClick: async () => {
                                                        setDownloadingBpId(b.id);
                                                        try {
                                                            const blob = await downloadSkillBlueprintSource(b.id);
                                                            const url = URL.createObjectURL(blob);
                                                            const a = document.createElement('a');
                                                            a.href = url;
                                                            a.download = b.sourceFileName ?? '细目表.xlsx';
                                                            a.click();
                                                            URL.revokeObjectURL(url);
                                                        }
                                                        finally {
                                                            setDownloadingBpId(undefined);
                                                        }
                                                    }, children: "\u6E90\u8868\u4E0B\u8F7D" }), _jsx(Popconfirm, { title: "\u5220\u9664\u8BE5\u7EC6\u76EE\u8868\u6279\u6B21\uFF1F", onConfirm: async () => {
                                                        await deleteSkillBlueprint(b.id);
                                                        message.success('已删除');
                                                        qc.invalidateQueries({ queryKey: ['skillgen-blueprints'] });
                                                    }, children: _jsx(Button, { size: "small", danger: true, children: "\u5220\u9664" }) })] })),
                                    },
                                ] })] })),
                },
                {
                    key: 'workbench',
                    label: '生成工作台',
                    // forceRender：停在其他页签时也保持挂载，进行中批次轮询不中断
                    forceRender: true,
                    children: (_jsx(SkillWorkbench, { workTypeOptions: workTypeOptions, blueprints: bpQuery.data?.items ?? [], presetBpId: presetBpId })),
                },
            ] }) }));
};
const SkillWorkbench = ({ workTypeOptions, blueprints, presetBpId }) => {
    const qc = useQueryClient();
    const paneActive = usePaneActive(); // 标签页不可见时暂停轮询（§3.6）
    const [blueprintId, setBlueprintId] = useState(presetBpId);
    const [modelConfigId, setModelConfigId] = useState();
    const [submitting, setSubmitting] = useState(false);
    const [activeBatchId, setActiveBatchId] = useState();
    const [historyPage, setHistoryPage] = useState(1);
    const [historyPageSize, setHistoryPageSize] = useState(10);
    const [draftPage, setDraftPage] = useState(1);
    const [draftPageSize, setDraftPageSize] = useState(20);
    const [allBatches, setAllBatches] = useState(false);
    const [reviewFilter, setReviewFilter] = useState('draft');
    const [selectedDraftIds, setSelectedDraftIds] = useState([]);
    const [editing, setEditing] = useState();
    const [publishTarget, setPublishTarget] = useState();
    React.useEffect(() => {
        if (presetBpId)
            setBlueprintId(presetBpId);
    }, [presetBpId]);
    const modelsQuery = useQuery({
        queryKey: ['skillgen-models'],
        queryFn: listSkillModelConfigs,
    });
    const modelOptions = useMemo(() => (modelsQuery.data ?? []).map((m) => ({
        value: m.id,
        label: `${m.modelName}（${m.modelId}${m.thinking ? ` · thinking=${m.thinking}` : ''}${m.reasoningEffort ? ` · effort=${m.reasoningEffort}` : ''}）`,
    })), [modelsQuery.data]);
    React.useEffect(() => {
        if (!modelConfigId && modelOptions.length) {
            const preferred = modelOptions.find((o) => o.label.includes('skill-gen')
                || o.label.includes('GLM-5.3'));
            setModelConfigId((preferred ?? modelOptions[0]).value);
        }
    }, [modelOptions, modelConfigId]);
    const bp = blueprints.find((b) => b.id === blueprintId);
    // 批次总览：进行中区轮询
    const activeQuery = useQuery({
        queryKey: ['skillgen-batches-active'],
        queryFn: () => listSkillGenBatches({ status: 'pending,running', page: 1, pageSize: 50 }),
        refetchInterval: (q) => {
            if (!paneActive)
                return false;
            const rows = q.state.data?.items ?? [];
            return rows.some((b) => b.status === 'pending' || b.status === 'running') ? 3000 : false;
        },
    });
    const historyQuery = useQuery({
        queryKey: ['skillgen-batches-history', historyPage, historyPageSize],
        // 终态批次服务端过滤（对齐 theorygen 工作台）：与进行中区互斥，
        // 避免提交时把"生成中"快照写进历史缓存、批次完成后永远显示旧状态
        queryFn: () => listSkillGenBatches({
            status: 'done,failed,paused', page: historyPage, pageSize: historyPageSize,
        }),
    });
    const activeItems = activeQuery.data?.items ?? [];
    const historyItems = historyQuery.data?.items ?? [];
    // 终态联动：批次从进行中区消失（完成/失败/暂停）时刷新历史区与草稿计数，
    // 否则全局 refetchOnWindowFocus=false + 历史区无轮询会让旧行永远停在"生成中"
    const prevActiveIdsRef = React.useRef(new Set());
    React.useEffect(() => {
        const current = new Set((activeQuery.data?.items ?? []).map((b) => b.id));
        const prev = prevActiveIdsRef.current;
        prevActiveIdsRef.current = current;
        if (prev.size === 0) {
            return; // 首次载入不触发
        }
        const left = [...prev].filter((id) => !current.has(id));
        if (left.length === 0) {
            return;
        }
        qc.invalidateQueries({ queryKey: ['skillgen-batches-history'] });
        qc.invalidateQueries({ queryKey: ['skillgen-drafts'] });
        left.forEach(async (id) => {
            try {
                const b = await getSkillGenProgress(id);
                if (b.status === 'failed') {
                    message.warning({ content: `生成批次已结束（失败）：${b.failReason ?? '未知原因'}`, duration: 6 });
                }
                else if (b.status === 'done') {
                    message.success(`生成批次已完成：已产出 ${b.totalQuestions ?? 0} 题`);
                }
            }
            catch {
                // 进度查询失败不打扰用户，列表刷新已足够
            }
        });
    }, [activeQuery.data, qc]);
    const draftsQuery = useQuery({
        queryKey: ['skillgen-drafts', allBatches ? 'all' : activeBatchId, reviewFilter, draftPage, draftPageSize],
        queryFn: () => listSkillDrafts({
            batchId: allBatches ? undefined : activeBatchId,
            reviewStatus: reviewFilter,
            page: draftPage,
            pageSize: draftPageSize,
        }),
        enabled: allBatches || !!activeBatchId,
    });
    // 切换批次/全部范围/审核状态时回到第 1 页
    React.useEffect(() => {
        setDraftPage(1);
    }, [allBatches, activeBatchId, reviewFilter]);
    const refreshAll = () => {
        qc.invalidateQueries({ queryKey: ['skillgen-batches-active'] });
        qc.invalidateQueries({ queryKey: ['skillgen-batches-history'] });
        qc.invalidateQueries({ queryKey: ['skillgen-drafts'] });
    };
    const doSubmit = async () => {
        if (!blueprintId || !modelConfigId) {
            message.warning('请选择细目表批次与模型');
            return;
        }
        setSubmitting(true);
        try {
            const batch = await submitSkillGenBatch(blueprintId, modelConfigId);
            message.success('生成任务已提交，可在下方批次表查看进度');
            setActiveBatchId(batch.id);
            refreshAll();
        }
        catch {
            // 预检失败（点数×5+已入库 < 等级下限）等错误提示由拦截器/后端消息展示
        }
        finally {
            setSubmitting(false);
        }
    };
    const openDrafts = (b) => {
        setActiveBatchId(b.id);
        setAllBatches(false);
        setSelectedDraftIds([]);
    };
    // 批次列（进行中区与历史区共用）
    const batchColumns = [
        {
            title: '批次', key: 'batch', width: 150,
            render: (_, b) => {
                const time = b.startedAt ?? b.createdAt ?? b.finishedAt;
                return (_jsxs("span", { children: [time ? dayjs(time).format('MM-DD HH:mm') : '-', _jsx("br", {}), _jsxs("span", { style: { color: '#999' }, children: ["\u6279\u6B21\u2026", b.id.slice(-8)] })] }));
            },
        },
        {
            title: '工种/等级', key: 'wl', width: 190,
            render: (_, b) => {
                const bpRow = blueprints.find((x) => x.id === b.blueprintId);
                return `${workTypeOptions.find((w) => w.value === (b.workTypeId ?? bpRow?.workTypeId))?.label ?? '-'} · ${LEVEL_NAME[b.level ?? bpRow?.level ?? 0] ?? '-'}`;
            },
        },
        {
            title: '模型', key: 'model', width: 150, ellipsis: true,
            render: (_, b) => `${b.modelId}${b.thinking ? `（思考 ${b.thinking === 'enabled' ? '开' : '关'}）` : ''}`,
        },
        {
            title: '状态', key: 'status', width: 110,
            render: (_, b) => {
                const tag = STATUS_TAG[b.status] ?? { color: 'default', text: b.status };
                const cls = b.status === 'pending' ? 'xmb-status-waiting'
                    : b.status === 'running' ? 'xmb-status-running' : '';
                const node = _jsx(Tag, { color: tag.color, className: cls, children: tag.text });
                return b.status === 'failed' && b.failReason
                    ? _jsx(Tooltip, { title: b.failReason, children: node }) : node;
            },
        },
        {
            title: '生成进度', key: 'progress', width: 230,
            render: (_, b) => {
                const pct = b.totalPoints ? Math.round(((b.donePoints ?? 0) * 100) / b.totalPoints) : 0;
                return (_jsxs("span", { children: [_jsx(Progress, { percent: pct, size: "small", status: b.status === 'running' ? 'active' : undefined, format: () => `${b.donePoints ?? 0}/${b.totalPoints ?? 0} 点` }), _jsxs("span", { style: { color: '#888' }, children: ["\u5DF2\u4EA7\u9898 ", b.totalQuestions ?? 0, " \u9898"] }), b.failedPoints ? (_jsx(Tooltip, { title: failedHint(b), children: _jsxs(Tag, { color: "error", style: { marginLeft: 4 }, children: ["\u5931\u8D25 ", b.failedPoints] }) })) : null] }));
            },
        },
        {
            title: '审核进度', key: 'review', width: 170,
            render: (_, b) => `通过 ${b.approvedCount ?? 0} · 待审 ${b.pendingReviewCount ?? 0} · 驳回 ${b.rejectedCount ?? 0}`,
        },
        {
            title: '入库', key: 'publish', width: 100,
            render: (_, b) => ((b.publishedCount ?? 0) > 0
                ? _jsxs(Tag, { color: "success", children: ["\u5DF2\u5165\u5E93 ", b.publishedCount] })
                : _jsx(Tag, { children: "\u672A\u5165\u5E93" })),
        },
        {
            title: '操作', key: 'op', width: 300, fixed: 'right',
            render: (_, b) => (_jsxs(Space, { size: 4, wrap: true, children: [_jsx(Button, { size: "small", type: "primary", onClick: () => openDrafts(b), children: "\u8349\u7A3F" }), (b.status === 'running' || b.status === 'pending') && (_jsx(Button, { size: "small", onClick: async () => {
                            await pauseSkillGenBatch(b.id);
                            message.success('已暂停');
                            refreshAll();
                        }, children: "\u6682\u505C" })), b.status === 'paused' && (_jsx(Button, { size: "small", onClick: async () => {
                            await resumeSkillGenBatch(b.id);
                            message.success('已继续');
                            refreshAll();
                        }, children: "\u7EE7\u7EED" })), (b.status === 'failed' || (b.status === 'done' && (b.donePoints ?? 0) < (b.totalPoints ?? 0))) && (_jsx(Button, { size: "small", onClick: async () => {
                            await retrySkillGenBatch(b.id);
                            message.success('已提交补跑');
                            refreshAll();
                        }, children: "\u8865\u8DD1\u7F3A\u53E3" })), _jsx(Button, { size: "small", onClick: () => setPublishTarget(b), children: "\u5165\u5E93" }), _jsx(Button, { size: "small", loading: false, onClick: async () => {
                            // 文件名由服务端统一下发（工种(等级)_操作技能试题_批次{短号}.xlsx），前端不再自行拼装
                            const { blob, filename } = await exportSkillDrafts(b.id);
                            const url = URL.createObjectURL(blob);
                            const a = document.createElement('a');
                            a.href = url;
                            a.download = filename || `操作技能试题_批次${b.id.slice(-8)}.xlsx`;
                            a.click();
                            URL.revokeObjectURL(url);
                        }, children: "\u5BFC\u51FA" })] })),
        },
    ];
    return (_jsxs("div", { children: [_jsx("div", { style: { padding: 12, border: '1px solid #f0f0f0', borderRadius: 8, marginBottom: 12 }, children: _jsxs(Space, { wrap: true, children: [_jsx(Select, { showSearch: true, optionFilterProp: "label", style: { width: 420 }, placeholder: "\u9009\u62E9\u7EC6\u76EE\u8868\u6279\u6B21", value: blueprintId, onChange: setBlueprintId, options: blueprints.map((b) => ({
                                value: b.id,
                                label: `${b.createdAt ? dayjs(b.createdAt).format('MM-DD HH:mm') : '时间未知'} · ${b.title}（${b.pointCount}点）`,
                            })) }), _jsx(Select, { style: { width: 340 }, placeholder: "\u9009\u62E9\u6A21\u578B", value: modelConfigId, onChange: setModelConfigId, options: modelOptions }), _jsxs("span", { style: { color: '#555' }, children: ["\u6BCF\u8003\u8BC4\u70B9\u56FA\u5B9A ", _jsx("b", { children: PER_POINT_QUOTA }), " \u9898", bp ? _jsxs(_Fragment, { children: ["\uFF0C\u672C\u6279\u9884\u8BA1\u4EA7\u51FA ", _jsx("b", { children: bp.pointCount * PER_POINT_QUOTA }), " \u9898\uFF08\u7B49\u7EA7\u4E0B\u9650 ", MIN_QUESTIONS[bp.level] ?? '-', " \u9898\uFF09"] }) : null] }), _jsx(Button, { type: "primary", loading: submitting, onClick: doSubmit, disabled: !bp, children: "\u63D0\u4EA4\u751F\u6210" })] }) }), _jsxs("div", { style: { fontWeight: 600, margin: '4px 0 8px' }, children: ["\u8FDB\u884C\u4E2D\u6279\u6B21", _jsx("span", { style: { color: '#888', fontWeight: 400, fontSize: 12 }, children: "\uFF08\u7B49\u5F85/\u751F\u6210\u4E2D\u7684\u6279\u6B21\u7F6E\u9876\u663E\u793A\uFF0C\u8FDB\u5EA6\u5B9E\u65F6\u5237\u65B0\uFF09" })] }), _jsx(Table, { rowKey: "id", size: "small", loading: activeQuery.isLoading, dataSource: activeItems, columns: batchColumns, pagination: false, locale: { emptyText: '当前没有进行中的批次' } }), _jsx("div", { style: { fontWeight: 600, margin: '16px 0 8px' }, children: "\u5386\u53F2\u6279\u6B21" }), _jsx(Table, { rowKey: "id", size: "small", loading: historyQuery.isLoading, dataSource: historyItems, columns: batchColumns, pagination: {
                    current: historyPage,
                    pageSize: historyPageSize,
                    total: historyQuery.data?.total,
                    showSizeChanger: true,
                    pageSizeOptions: [10, 20, 50],
                    showTotal: (t) => `共 ${t} 条`,
                    onChange: (p, s) => {
                        setHistoryPage(s !== historyPageSize ? 1 : p);
                        setHistoryPageSize(s);
                    },
                } }), (allBatches || activeBatchId) ? (_jsxs("div", { style: { border: '1px solid #f0f0f0', borderRadius: 8, padding: 12 }, children: [_jsxs(Space, { wrap: true, style: { marginBottom: 8 }, children: [_jsx("b", { children: "\u8BD5\u9898\u8349\u7A3F" }), _jsx(Radio.Group, { value: allBatches, onChange: (e) => { setAllBatches(e.target.value); setSelectedDraftIds([]); }, optionType: "button", options: [{ label: '当前批次', value: false }, { label: '全部批次', value: true }] }), _jsx(Select, { style: { width: 140 }, value: reviewFilter, onChange: (v) => { setReviewFilter(v); setSelectedDraftIds([]); }, options: [
                                    { value: undefined, label: '全部状态' },
                                    { value: 'draft', label: '待审' },
                                    { value: 'approved', label: '已通过' },
                                    { value: 'rejected', label: '已驳回' },
                                ] }), _jsx(Button, { size: "small", type: "primary", disabled: !selectedDraftIds.length, onClick: async () => {
                                    await reviewSkillDrafts(selectedDraftIds, true);
                                    message.success('已通过');
                                    setSelectedDraftIds([]);
                                    refreshAll();
                                }, children: "\u6279\u91CF\u901A\u8FC7" }), _jsx(Button, { size: "small", danger: true, disabled: !selectedDraftIds.length, onClick: async () => {
                                    await reviewSkillDrafts(selectedDraftIds, false);
                                    message.success('已驳回');
                                    setSelectedDraftIds([]);
                                    refreshAll();
                                }, children: "\u6279\u91CF\u9A73\u56DE" })] }), _jsx(Table, { rowKey: "id", size: "small", loading: draftsQuery.isLoading, dataSource: draftsQuery.data?.items ?? [], pagination: {
                            current: draftPage,
                            pageSize: draftPageSize,
                            total: draftsQuery.data?.total,
                            showSizeChanger: true,
                            pageSizeOptions: [10, 20, 50],
                            showTotal: (t) => `共 ${t} 条`,
                            onChange: (p, s) => {
                                setDraftPage(s !== draftPageSize ? 1 : p);
                                setDraftPageSize(s);
                            },
                        }, rowSelection: {
                            selectedRowKeys: selectedDraftIds,
                            onChange: (keys) => setSelectedDraftIds(keys),
                            getCheckboxProps: (r) => ({ disabled: r.publishStatus === 1 }),
                        }, expandable: {
                            expandedRowRender: (d) => _jsx(DraftDetail, { draft: d }),
                        }, columns: [
                            { title: '试题名称', dataIndex: 'questionName', ellipsis: true },
                            {
                                title: '分值/权重', key: 'score', width: 110,
                                render: (_, d) => `${d.score ?? 100} 分${d.weight != null ? ` / ${d.weight}%` : ''}`,
                            },
                            { title: '考核时间', key: 'duration', width: 90, render: (_, d) => `${d.durationMinutes ?? '-'} min` },
                            { title: '形式', dataIndex: 'examForm', width: 80 },
                            {
                                title: '难度', key: 'diff', width: 70,
                                render: (_, d) => d.difficulty ?? 3,
                            },
                            {
                                title: '评分合计', key: 'sum', width: 90,
                                render: (_, d) => {
                                    const sum = scoringSum(parseScoring(d.scoringItems));
                                    return _jsx(Tag, { color: Math.abs(sum - 100) < 0.01 ? 'success' : 'error', children: sum });
                                },
                            },
                            {
                                title: '状态', key: 'review', width: 90,
                                render: (_, d) => {
                                    const tag = REVIEW_TAG[d.reviewStatus] ?? { color: 'default', text: d.reviewStatus };
                                    return _jsx(Tag, { color: tag.color, children: tag.text });
                                },
                            },
                            {
                                title: '入库', key: 'pub', width: 80,
                                render: (_, d) => (d.publishStatus === 1 ? _jsx(Tag, { color: "success", children: "\u5DF2\u5165\u5E93" }) : _jsx(Tag, { children: "\u672A\u5165\u5E93" })),
                            },
                            {
                                title: '操作', key: 'op', width: 90,
                                render: (_, d) => (_jsx(Button, { size: "small", disabled: d.publishStatus === 1, onClick: () => setEditing(d), children: "\u7F16\u8F91" })),
                            },
                        ] })] })) : (_jsx(Alert, { type: "info", showIcon: true, message: "\u70B9\u51FB\u6279\u6B21\u8868\u884C\u5185\u300C\u8349\u7A3F\u300D\u8FDB\u5165\u5BA1\u6838\u9762\u677F" })), editing && (_jsx(DraftEditDrawer, { draft: editing, onClose: () => setEditing(undefined), onSaved: () => { setEditing(undefined); refreshAll(); } })), publishTarget && (_jsx(PublishModal, { batch: publishTarget, onClose: () => setPublishTarget(undefined), onPublished: () => { setPublishTarget(undefined); refreshAll(); } }))] }));
};
// ---------------------------------------------------------------------------
// 草稿展开行：三件套 + 评分表
// ---------------------------------------------------------------------------
const DraftDetail = ({ draft }) => {
    const scoring = parseScoring(draft.scoringItems);
    return (_jsxs("div", { style: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }, children: [_jsxs("div", { children: [_jsx("b", { children: "\u5177\u4F53\u8003\u6838\u8981\u6C42" }), _jsx("div", { style: { whiteSpace: 'pre-wrap' }, children: draft.requirements }), _jsx("b", { children: "\u5426\u5B9A\u9879" }), _jsx("div", { style: { whiteSpace: 'pre-wrap' }, children: draft.negatives }), _jsx("b", { children: "\u8003\u751F\u51C6\u5907" }), _jsx("div", { style: { whiteSpace: 'pre-wrap' }, children: draft.examineePrepare })] }), _jsxs("div", { children: [_jsx("b", { children: "\u8003\u573A\u51C6\u5907\uFF08\u542B\u8BBE\u5907\u8BBE\u65BD\uFF09" }), _jsx("div", { style: { whiteSpace: 'pre-wrap' }, children: draft.sitePreparation })] }), _jsxs("div", { style: { gridColumn: '1 / span 2' }, children: [_jsxs("b", { children: ["\u8BC4\u5206\u8BB0\u5F55\u8868\uFF08\u5408\u8BA1 ", scoringSum(scoring), " \u5206\uFF09"] }), _jsx(Table, { rowKey: (_, i) => String(i), size: "small", pagination: false, dataSource: scoring, columns: [
                            { title: '评分要素', dataIndex: 'item' },
                            { title: '配分', dataIndex: 'score', width: 70 },
                            { title: '评分标准', dataIndex: 'scoreDesc' },
                            { title: '最大扣分', dataIndex: 'maxDeduct', width: 90 },
                            { title: '步长', dataIndex: 'step', width: 70 },
                        ] })] })] }));
};
// ---------------------------------------------------------------------------
// 编辑抽屉（含评分行编辑器 + 配分合计校验；保存后自动退回待审）
// ---------------------------------------------------------------------------
const DraftEditDrawer = ({ draft, onClose, onSaved }) => {
    const [questionName, setQuestionName] = useState(draft.questionName);
    const [examForm, setExamForm] = useState(draft.examForm ?? '实操');
    const [durationMinutes, setDurationMinutes] = useState(draft.durationMinutes ?? 20);
    const [difficulty, setDifficulty] = useState(draft.difficulty ?? 3);
    const [requirements, setRequirements] = useState(draft.requirements ?? '');
    const [negatives, setNegatives] = useState(draft.negatives ?? '');
    const [examineePrepare, setExamineePrepare] = useState(draft.examineePrepare ?? '');
    const [sitePreparation, setSitePreparation] = useState(draft.sitePreparation ?? '');
    const [scoring, setScoring] = useState(parseScoring(draft.scoringItems));
    const [saving, setSaving] = useState(false);
    const sum = scoringSum(scoring);
    const save = async () => {
        if (Math.abs(sum - 100) > 0.01) {
            message.error(`评分表配分合计 ${sum}，必须等于 100`);
            return;
        }
        setSaving(true);
        try {
            await editSkillDraft(draft.id, {
                questionName, examForm, durationMinutes, difficulty, requirements, negatives,
                examineePrepare, sitePreparation, scoringItems: JSON.stringify(scoring),
            });
            message.success('已保存，状态已退回待审核，请重新审核');
            onSaved();
        }
        finally {
            setSaving(false);
        }
    };
    return (_jsx(Drawer, { title: `编辑试题草稿（${draft.questionName}）`, width: 760, open: true, onClose: onClose, extra: _jsx(Button, { type: "primary", loading: saving, onClick: save, children: "\u4FDD\u5B58" }), children: _jsxs(Space, { direction: "vertical", style: { width: '100%' }, size: "middle", children: [_jsxs("div", { children: [_jsx("div", { children: "\u8BD5\u9898\u540D\u79F0" }), _jsx(Input, { value: questionName, onChange: (e) => setQuestionName(e.target.value) })] }), _jsxs(Space, { children: [_jsx("span", { children: "\u8003\u6838\u5F62\u5F0F" }), _jsx(Input, { style: { width: 120 }, value: examForm, onChange: (e) => setExamForm(e.target.value) }), _jsx("span", { children: "\u8003\u6838\u65F6\u95F4\uFF08\u5206\u949F\uFF09" }), _jsx(InputNumber, { min: 5, max: 240, value: durationMinutes, onChange: setDurationMinutes }), _jsx("span", { children: "\u96BE\u5EA6" }), _jsx(InputNumber, { min: 1, max: 5, value: difficulty, onChange: setDifficulty })] }), _jsxs("div", { children: [_jsx("div", { children: "\u5177\u4F53\u8003\u6838\u8981\u6C42" }), _jsx(Input.TextArea, { rows: 3, value: requirements, onChange: (e) => setRequirements(e.target.value) })] }), _jsxs("div", { children: [_jsx("div", { children: "\u5426\u5B9A\u9879" }), _jsx(Input.TextArea, { rows: 3, value: negatives, onChange: (e) => setNegatives(e.target.value) })] }), _jsxs("div", { children: [_jsx("div", { children: "\u8003\u751F\u51C6\u5907" }), _jsx(Input.TextArea, { rows: 2, value: examineePrepare, onChange: (e) => setExamineePrepare(e.target.value) })] }), _jsxs("div", { children: [_jsx("div", { children: "\u8003\u573A\u51C6\u5907\uFF08JSON \u7ED3\u6784\uFF0C\u542B\u8BBE\u5907\u8BBE\u65BD\u8868\uFF09" }), _jsx(Input.TextArea, { rows: 3, value: sitePreparation, onChange: (e) => setSitePreparation(e.target.value) })] }), _jsxs("div", { children: [_jsxs(Space, { children: [_jsx("b", { children: "\u8BC4\u5206\u8BB0\u5F55\u8868" }), _jsxs(Tag, { color: Math.abs(sum - 100) < 0.01 ? 'success' : 'error', children: ["\u914D\u5206\u5408\u8BA1 ", sum] }), _jsx(Button, { size: "small", onClick: () => setScoring([...scoring, { item: '', score: '', scoreDesc: '', maxDeduct: '', step: '' }]), children: "\u589E\u52A0\u884C" })] }), scoring.map((row, idx) => (_jsxs(Space, { style: { width: '100%', marginTop: 6 }, align: "start", wrap: true, children: [_jsx(Input, { style: { width: 150 }, placeholder: "\u8BC4\u5206\u8981\u7D20", value: row.item, onChange: (e) => setScoring(scoring.map((r, i) => (i === idx ? { ...r, item: e.target.value } : r))) }), _jsx(Input, { style: { width: 70 }, placeholder: "\u914D\u5206", value: row.score, onChange: (e) => setScoring(scoring.map((r, i) => (i === idx ? { ...r, score: e.target.value } : r))) }), _jsx(Input, { style: { width: 260 }, placeholder: "\u8BC4\u5206\u6807\u51C6", value: row.scoreDesc, onChange: (e) => setScoring(scoring.map((r, i) => (i === idx ? { ...r, scoreDesc: e.target.value } : r))) }), _jsx(Input, { style: { width: 80 }, placeholder: "\u6700\u5927\u6263\u5206", value: row.maxDeduct, onChange: (e) => setScoring(scoring.map((r, i) => (i === idx ? { ...r, maxDeduct: e.target.value } : r))) }), _jsx(Input, { style: { width: 60 }, placeholder: "\u6B65\u957F", value: row.step, onChange: (e) => setScoring(scoring.map((r, i) => (i === idx ? { ...r, step: e.target.value } : r))) }), _jsx(Button, { size: "small", danger: true, onClick: () => setScoring(scoring.filter((_, i) => i !== idx)), children: "\u5220\u9664" })] }, idx)))] })] }) }));
};
// ---------------------------------------------------------------------------
// 入库弹窗（硬闸门：累计进度 + 缺口展示，不足置灰）
// ---------------------------------------------------------------------------
const PublishModal = ({ batch, onClose, onPublished }) => {
    const [taskMode, setTaskMode] = useState('create');
    const [taskId, setTaskId] = useState();
    const [publishing, setPublishing] = useState(false);
    const preview = useQuery({
        queryKey: ['skillgen-publish-preview', batch.id],
        queryFn: () => getSkillPublishPreview(batch.id),
    });
    const p = preview.data;
    return (_jsx(Modal, { title: "\u8BD5\u9898\u5165\u5E93\uFF08\u5199\u5165\u5B9E\u64CD\u547D\u9898\u4EFB\u52A1\uFF09", open: true, onCancel: onClose, okText: "\u786E\u8BA4\u5165\u5E93", okButtonProps: { disabled: !p?.canPublish, loading: publishing }, onOk: async () => {
            setPublishing(true);
            try {
                const result = await publishSkillDrafts(batch.id, taskMode, taskId);
                message.success(`已入库 ${result.published} 题（累计 ${result.cumulativeTotal}，其中 AI 生成 ${result.cumulativeAiTotal}）`);
                onPublished();
            }
            finally {
                setPublishing(false);
            }
        }, children: _jsxs(Space, { direction: "vertical", style: { width: '100%' }, children: [_jsxs("div", { children: ["\u672C\u6B21\u53EF\u5165\u5E93\uFF08\u5DF2\u5BA1\u6838\u901A\u8FC7\uFF09\uFF1A", _jsx("b", { children: p?.approvedCount ?? 0 }), " \u9898"] }), _jsxs("div", { children: ["\u8BE5\u5DE5\u79CD\u7B49\u7EA7\u7D2F\u8BA1\u8FDB\u5EA6\uFF1A", _jsx("b", { children: p?.afterTotal ?? 0 }), " / \u4E0B\u9650 ", p?.floor ?? '-', " \u9898\uFF08\u5165\u5E93\u524D ", p?.cumulativeTotal ?? 0, " \u9898\uFF0C \u5176\u4E2D AI \u751F\u6210 ", p?.cumulativeAiTotal ?? 0, " \u9898\uFF09"] }), p && p.gap > 0 && (_jsx(Alert, { type: "warning", showIcon: true, message: `还差 ${p.gap} 题才达到本等级题库总量下限，暂不可入库：请继续生成并审核通过后再入库。` })), p && p.approvedCount === 0 && (_jsx(Alert, { type: "info", showIcon: true, message: "\u5F53\u524D\u6CA1\u6709\u5BA1\u6838\u901A\u8FC7\u7684\u8BD5\u9898\uFF0C\u8BF7\u5148\u5728\u8349\u7A3F\u9762\u677F\u5BA1\u6838\u901A\u8FC7\u540E\u518D\u5165\u5E93\u3002" })), _jsxs("div", { children: [_jsx("span", { children: "\u4EFB\u52A1\u6A21\u5F0F\uFF1A" }), _jsx(Radio.Group, { value: taskMode, onChange: (e) => setTaskMode(e.target.value), options: [
                                { label: '新建命题任务', value: 'create' },
                                { label: '追加到现有任务', value: 'append' },
                            ] }), taskMode === 'append' && (_jsx(Input, { style: { width: 220, marginLeft: 8 }, placeholder: "\u586B\u5199\u4EFB\u52A1 ID", value: taskId, onChange: (e) => setTaskId(e.target.value) }))] })] }) }));
};
export default SkillQuestionGenPage;

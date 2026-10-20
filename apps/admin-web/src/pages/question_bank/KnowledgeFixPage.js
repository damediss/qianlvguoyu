import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
/**
 * 知识归属修正工作台（方案 §7.4）：三页签
 * ① 去重清理（生成清单 → 软删除 → 可回滚）
 * ② 知识修正（LLM 三采样自一致分类 → 改挂/保留/存疑 清单 → 置信分层写回）
 * ③ 质量报告（自一致率/置信分布/存疑规模）
 */
import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Descriptions, Input, InputNumber, message, Modal, Popconfirm, Progress, Space, Table, Tabs, Tag, Typography, } from 'antd';
import { dedupStats, generateDedupList, applyDedup, rollbackDedup, submitClassify, resetClassify, pauseClassify, classifyProgress, fixList, exportFixList, applyFix, adjudicateFix, adjudicateFixBatch, qualityReport, } from './knowledgeFixApi';
import { usePaneActive } from '../../layouts/TabKeepAlive';
const STATUS_TAG = {
    apply: { color: 'success', text: '改挂' },
    keep: { color: 'processing', text: '保留' },
    flag: { color: 'warning', text: '存疑' },
    pending: { color: 'default', text: '待分类' },
    applied: { color: 'success', text: '已写回' },
};
/** 去重备份表名规则（后端同样的前缀+日期守卫；前端先校验，避免手误打错表名） */
const DEDUP_BACKUP_PATTERN = /^exam_exam_detail_bak_dedup_\d{8}$/;
const LAST_DEDUP_BACKUP_KEY = 'guoyu_kfix_last_dedup_backup';
/** 判断批次是否仍在跑（进度轮询持续条件） */
const batchRunning = (b) => !!b && (b.status === 'running' || b.status === 'pending' || b.status === 'pausing');
const KnowledgeFixPage = () => {
    const qc = useQueryClient();
    const paneActive = usePaneActive(); // 标签页不可见时暂停轮询（§3.6）
    // ---------------- 去重清理 ----------------
    const [dedupGenerating, setDedupGenerating] = useState(false);
    const [dedupApplying, setDedupApplying] = useState(false);
    const [dedupRollingBack, setDedupRollingBack] = useState(false);
    const [lastDedupBackup, setLastDedupBackup] = useState(() => localStorage.getItem(LAST_DEDUP_BACKUP_KEY));
    const [rollbackOpen, setRollbackOpen] = useState(false);
    const [rollbackTable, setRollbackTable] = useState('');
    const dedupStatsQuery = useQuery({
        queryKey: ['knowledgefix-dedup-stats'],
        queryFn: dedupStats,
    });
    const doGenerateDedup = async () => {
        setDedupGenerating(true);
        try {
            const stats = await generateDedupList();
            message.success(`去重清单已生成：重复题组 ${stats.dupGroups}，待软删 ${stats.deleteRows} 行` +
                (stats.manualReviewRows > 0 ? `，人工复核 ${stats.manualReviewRows} 行` : ''));
            qc.invalidateQueries({ queryKey: ['knowledgefix-dedup-stats'] });
        }
        catch {
            /* 拦截器统一提示 */
        }
        finally {
            setDedupGenerating(false);
        }
    };
    const doApplyDedup = async () => {
        setDedupApplying(true);
        try {
            const r = await applyDedup();
            setLastDedupBackup(r.backupTable);
            if (r.backupTable) {
                localStorage.setItem(LAST_DEDUP_BACKUP_KEY, r.backupTable);
            }
            message.success(`软删除完成：${r.affectedRows} 行（备份表 ${r.backupTable ?? '-'}，可回滚）`);
            qc.invalidateQueries({ queryKey: ['knowledgefix-dedup-stats'] });
        }
        catch {
            /* 拦截器统一提示 */
        }
        finally {
            setDedupApplying(false);
        }
    };
    /** 打开回滚弹窗：默认填入上次使用的备份表（跨会话保留），否则留空待填 */
    const openRollbackDedup = () => {
        setRollbackTable(lastDedupBackup ?? localStorage.getItem(LAST_DEDUP_BACKUP_KEY) ?? '');
        setRollbackOpen(true);
    };
    const doRollbackDedup = async () => {
        const table = rollbackTable.trim();
        if (!DEDUP_BACKUP_PATTERN.test(table)) {
            message.warning('备份表名格式应为 exam_exam_detail_bak_dedup_YYYYMMDD');
            return;
        }
        setDedupRollingBack(true);
        try {
            const n = await rollbackDedup(table);
            localStorage.setItem(LAST_DEDUP_BACKUP_KEY, table);
            setLastDedupBackup(table);
            setRollbackOpen(false);
            message.success(`已按 ${table} 回滚 ${n} 行（清单已标记 rolled_back）`);
            qc.invalidateQueries({ queryKey: ['knowledgefix-dedup-stats'] });
        }
        catch {
            /* 拦截器统一提示 */
        }
        finally {
            setDedupRollingBack(false);
        }
    };
    const dedupColumns = [
        { title: '指标', dataIndex: 'k', key: 'k' },
        { title: '数值', dataIndex: 'v', key: 'v' },
    ];
    const s = dedupStatsQuery.data;
    const dedupRows = [
        { k: '5 月以来导入行数（存活）', v: s?.mayTotalRows ?? '-' },
        { k: '重复题组数（同课程口径）', v: s?.dupGroups ?? '-' },
        { k: '待执行删除行', v: s?.deleteRows ?? '-' },
        { k: '已软删行（可回滚）', v: s?.alreadyDeletedRows ?? '-' },
        { k: '人工复核行（有引用）', v: s?.manualReviewRows ?? '-' },
    ];
    // ---------------- 知识修正 ----------------
    const [fixApplying, setFixApplying] = useState(false);
    /** 存疑行人工定夺勾选（仅存疑页签可勾选） */
    const [selectedRowKeys, setSelectedRowKeys] = useState([]);
    const progressQuery = useQuery({
        queryKey: ['knowledgefix-progress'],
        queryFn: classifyProgress,
        refetchInterval: (q) => (paneActive && batchRunning(q.state.data) ? 3000 : false),
    });
    const batch = progressQuery.data;
    // 待写回改挂行数（实时）：人工定夺会把 flag 行翻转成 apply，批次卡的 applyCnt 是批次时点快照不更新，
    // 写回按钮必须按当前行数判断（否则定夺后按钮恒灰）
    const applyCountQuery = useQuery({
        queryKey: ['knowledgefix-apply-count'],
        queryFn: () => fixList({ status: 'apply', page: 1, pageSize: 1 }),
        enabled: !!batch,
    });
    const applyCount = applyCountQuery.data?.total ?? 0;
    const reportQuery = useQuery({
        queryKey: ['knowledgefix-report'],
        queryFn: qualityReport,
    });
    const report = reportQuery.data ?? batch;
    const [fixStatus, setFixStatus] = useState();
    const [fixPage, setFixPage] = useState(1);
    const [fixPageSize, setFixPageSize] = useState(20);
    const fixQuery = useQuery({
        queryKey: ['knowledgefix-fix-list', fixStatus, fixPage, fixPageSize],
        queryFn: () => fixList({ status: fixStatus, page: fixPage, pageSize: fixPageSize }),
    });
    const doSubmitClassify = async (scope) => {
        try {
            const b = await submitClassify(scope);
            message.success(`分类批次已提交：${b.total} 行（三采样自一致 + 低置信升级旗舰）`);
            qc.invalidateQueries({ queryKey: ['knowledgefix-progress'] });
        }
        catch {
            /* 拦截器统一提示 */
        }
    };
    // 范围提交（§13 步骤 8 小批次实测）：课程 ID + 行数上限；暂停续跑须携带相同参数
    const [scopeCourseId, setScopeCourseId] = useState();
    const [scopeLimit, setScopeLimit] = useState();
    const doScopeSubmit = async () => {
        if (scopeCourseId == null && scopeLimit == null) {
            message.warning('请至少填写课程 ID 或行数上限（留空则全量提交）');
            return;
        }
        await doSubmitClassify({
            courseId: scopeCourseId,
            limit: scopeLimit,
        });
    };
    const doResetClassify = async () => {
        try {
            const n = await resetClassify();
            message.success(`已复位 ${n} 行（存疑/未分类 → 待分类），可重新提交批次`);
            qc.invalidateQueries({ queryKey: ['knowledgefix-progress'] });
            qc.invalidateQueries({ queryKey: ['knowledgefix-fix-list'] });
        }
        catch {
            /* 拦截器统一提示 */
        }
    };
    /** 人工暂停（FIX-10/v1.7）：停止派发新行，在飞行的行跑完后批次置 paused；继续=再点提交 */
    const doPause = async () => {
        try {
            await pauseClassify();
            message.success('暂停请求已登记，正在等待已派发的行跑完…');
            qc.invalidateQueries({ queryKey: ['knowledgefix-progress'] });
        }
        catch {
            /* 拦截器统一提示 */
        }
    };
    const doApplyFix = async () => {
        setFixApplying(true);
        try {
            const r = await applyFix();
            message.success(`知识修正写回完成：${r.affectedRows} 行（备份表 ${r.backupTable ?? '-'}）`);
            qc.invalidateQueries({ queryKey: ['knowledgefix-fix-list'] });
            qc.invalidateQueries({ queryKey: ['knowledgefix-report'] });
            qc.invalidateQueries({ queryKey: ['knowledgefix-apply-count'] });
        }
        catch {
            /* 拦截器统一提示 */
        }
        finally {
            setFixApplying(false);
        }
    };
    /** 存疑行人工定夺结果提示：成功/跳过分别报，跳过带原因（最多展示 3 条） */
    const reportAdjudicate = (r) => {
        if (r.skipped.length === 0) {
            message.success(`已定夺 ${r.succeeded} 行`);
        }
        else {
            const reasons = r.skipped.slice(0, 3).map((s) => s.reason).join('；');
            message.warning(`定夺成功 ${r.succeeded} 行，跳过 ${r.skipped.length} 行：${reasons}${r.skipped.length > 3 ? '…' : ''}`);
        }
        setSelectedRowKeys([]);
        qc.invalidateQueries({ queryKey: ['knowledgefix-fix-list'] });
        qc.invalidateQueries({ queryKey: ['knowledgefix-apply-count'] });
    };
    const doAdjudicate = async (rowId, action) => {
        try {
            reportAdjudicate(await adjudicateFix(rowId, action));
        }
        catch {
            /* 拦截器统一提示 */
        }
    };
    const doBatchAdjudicate = async (action) => {
        try {
            reportAdjudicate(await adjudicateFixBatch(selectedRowKeys, action));
        }
        catch {
            /* 拦截器统一提示 */
        }
    };
    const doExportFix = async () => {
        const blob = await exportFixList();
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'knowledge-fix-list.xlsx';
        a.click();
        URL.revokeObjectURL(url);
    };
    const fixColumns = [
        { title: 'detail_id', dataIndex: 'detailId', key: 'detailId', width: 90 },
        { title: '题干摘要', dataIndex: 'stemPreview', key: 'stemPreview', ellipsis: true,
            render: (v) => (v ? v : '-') },
        { title: '原知识点', dataIndex: 'oldKnowledgeId', key: 'oldKnowledgeId', width: 190,
            render: (v, r) => `${v}${r.oldKnowledgeTitle ? ' ' + r.oldKnowledgeTitle : ''}` },
        { title: '新知识点', dataIndex: 'newKnowledgeId', key: 'newKnowledgeId', width: 190,
            render: (v, r) => (v == null ? '-' : `${v}${r.newKnowledgeTitle ? ' ' + r.newKnowledgeTitle : ''}`) },
        { title: '置信', dataIndex: 'confidence', key: 'confidence', width: 70,
            render: (v) => (v == null ? '-' : v.toFixed(2)) },
        { title: '依据', dataIndex: 'basis', key: 'basis', width: 110,
            render: (v) => {
                if (v == null || v === '')
                    return '-';
                const jevBasis = v === 'jev' || v === 'jev-l2-direct';
                return jevBasis ? _jsx(Tag, { color: "purple", children: v }) : v;
            } },
        { title: '理由', dataIndex: 'llmReason', key: 'llmReason', ellipsis: true,
            render: (v, record) => (v ? v : (record.basis === 'jev' || record.basis === 'jev-l2-direct' ? '（概率判定无理由）' : '-')) },
        { title: '状态', dataIndex: 'status', key: 'status', width: 90,
            render: (v) => {
                const t = STATUS_TAG[v] ?? { color: 'default', text: v };
                return _jsx(Tag, { color: t.color, children: t.text });
            } },
        { title: '操作', key: 'op', width: 120,
            render: (_, r) => (r.status === 'flag' ? (_jsxs(Space, { size: 0, children: [_jsx(Popconfirm, { title: "\u5B9A\u593A\u4E3A\u6539\u6302\uFF1F", description: `采用新知识点 ${r.newKnowledgeId ?? '-'}（${r.newKnowledgeTitle ?? ''}），定夺后需点「执行写回」才写进题库`, onConfirm: () => doAdjudicate(r.rowId, 'apply'), children: _jsx(Button, { type: "link", size: "small", children: "\u6539\u6302" }) }), _jsx(Popconfirm, { title: "\u5B9A\u593A\u4E3A\u4FDD\u7559\uFF1F", description: "\u7EF4\u6301\u539F\u77E5\u8BC6\u70B9\u4E0D\u53D8\uFF08\u539F\u8282\u70B9\u5DF2\u5220\u7684\u884C\u4F1A\u88AB\u62D2\u7EDD\uFF09", onConfirm: () => doAdjudicate(r.rowId, 'keep'), children: _jsx(Button, { type: "link", size: "small", children: "\u4FDD\u7559" }) })] })) : '-') },
    ];
    return (_jsxs("div", { className: "ximu-page", children: [_jsx(Alert, { type: "warning", showIcon: true, message: "\u751F\u4EA7\u6570\u636E\u64CD\u4F5C\uFF1A\u6240\u6709\u5199\u64CD\u4F5C\u81EA\u52A8\u5907\u4EFD\u3001\u53EF\u6574\u4F53\u56DE\u6EDA\uFF1B\u6267\u884C\u524D\u8BF7\u786E\u8BA4\u5F53\u524D\u7A97\u53E3\u4E3A\u5BA1\u6279\u5141\u8BB8\u7684\u65F6\u6BB5\u3002", style: { marginBottom: 12 } }), _jsx(Tabs, { items: [
                    {
                        key: 'dedup', label: '去重清理',
                        children: (_jsxs(_Fragment, { children: [_jsxs(Space, { style: { marginBottom: 16 }, wrap: true, children: [_jsx(Button, { loading: dedupGenerating, onClick: doGenerateDedup, children: dedupGenerating ? '清单生成中…' : '生成/刷新去重清单' }), _jsx(Popconfirm, { title: "\u786E\u8BA4\u6267\u884C\u8F6F\u5220\u9664\uFF1F", description: "\u5C06\u6309\u6E05\u5355\u628A\u5197\u4F59\u526F\u672C\u7F6E\u4E3A deleted=1\uFF08\u5DF2\u81EA\u52A8\u5907\u4EFD\uFF0C\u53EF\u56DE\u6EDA\uFF09", onConfirm: doApplyDedup, disabled: dedupApplying || !s || (s?.deleteRows ?? 0) === 0, children: _jsx(Button, { danger: true, loading: dedupApplying, disabled: dedupApplying || !s || (s?.deleteRows ?? 0) === 0, children: "\u6267\u884C\u8F6F\u5220\u9664\uFF08deleted=1\uFF09" }) }), _jsx(Button, { danger: true, onClick: openRollbackDedup, disabled: dedupRollingBack, children: "\u56DE\u6EDA\u53BB\u91CD\uFF08\u6309\u5907\u4EFD\u8868\uFF09" })] }), _jsxs(Modal, { title: "\u56DE\u6EDA\u53BB\u91CD\u8F6F\u5220\u9664", open: rollbackOpen, onCancel: () => setRollbackOpen(false), onOk: doRollbackDedup, okText: "\u786E\u8BA4\u56DE\u6EDA", okButtonProps: { danger: true, loading: dedupRollingBack }, destroyOnHidden: true, children: [_jsx(Alert, { type: "error", showIcon: true, style: { marginBottom: 12 }, message: "\u56DE\u6EDA\u4F1A\u628A\u8BE5\u5907\u4EFD\u8868\u8986\u76D6\u5230\u7684\u884C\u6062\u590D\u4E3A deleted=0\uFF0C\u5E76\u628A\u672C\u6B21\u53BB\u91CD\u6E05\u5355\u6807\u8BB0\u4E3A rolled_back\u3002" }), _jsx("div", { style: { marginBottom: 8 }, children: "\u5907\u4EFD\u8868\u540D\uFF08\u6BCF\u6B21\u6267\u884C\u8F6F\u5220\u9664\u81EA\u52A8\u751F\u6210\uFF0C\u53EF\u56DE\u6EDA\uFF09" }), _jsx(Input, { value: rollbackTable, onChange: (e) => setRollbackTable(e.target.value), placeholder: "exam_exam_detail_bak_dedup_YYYYMMDD", allowClear: true }), _jsx("div", { style: { marginTop: 8, color: '#8c8c8c', fontSize: 12 }, children: "\u67E5\u6700\u8FD1\u5907\u4EFD\u8868\uFF1ASELECT table_name FROM information_schema.tables WHERE table_name LIKE 'exam_exam_detail_bak_dedup_%' ORDER BY create_time DESC" })] }), dedupApplying && _jsx(Progress, { percent: 70, status: "active", style: { marginBottom: 12 } }), _jsxs(Descriptions, { column: 2, size: "small", style: { marginBottom: 12 }, title: "\u7EDF\u8BA1", children: [_jsx(Descriptions.Item, { label: "5 \u6708\u4EE5\u6765\u5BFC\u5165", children: s?.mayTotalRows ?? '-' }), _jsx(Descriptions.Item, { label: "\u91CD\u590D\u9898\u7EC4", children: s?.dupGroups ?? '-' }), _jsx(Descriptions.Item, { label: "\u5F85\u5220\u5197\u4F59", children: s?.deleteRows ?? '-' }), _jsx(Descriptions.Item, { label: "\u5DF2\u8F6F\u5220", children: s?.alreadyDeletedRows ?? '-' }), _jsx(Descriptions.Item, { label: "\u4EBA\u5DE5\u590D\u6838", children: s?.manualReviewRows ?? '-' })] }), _jsx(Alert, { type: "info", showIcon: true, style: { marginBottom: 12 }, message: "\u5224\u91CD\u53E3\u5F84\uFF1A\u540C\u4E00\u8BFE\u7A0B\u5185\u540C\u9898\u591A\u4EFD\u624D\u7B97\u91CD\u590D\uFF1B\u8DE8\u8BFE\u7A0B\u540C\u9898\u5404\u81EA\u4FDD\u7559\uFF08\u8BFE\u7A0B\u662F\u9898\u5E93\u4F7F\u7528\u5355\u4F4D\uFF09\u3002\u65E0\u77E5\u8BC6\u5F52\u5C5E\u7684\u884C\u6309\u72EC\u7ACB\u6876\u5904\u7406\u3002" }), _jsx(Table, { rowKey: "k", size: "small", dataSource: dedupRows, columns: dedupColumns, pagination: false })] })),
                    },
                    {
                        key: 'fix', label: '知识修正',
                        children: (_jsxs(_Fragment, { children: [_jsxs(Space, { style: { marginBottom: 16 }, wrap: true, children: [_jsx(Button, { type: "primary", onClick: () => doSubmitClassify(), disabled: batchRunning(batch), children: batch?.status === 'paused' ? '提交 LLM 分类批次（全量）' : '提交 LLM 分类批次' }), _jsx(InputNumber, { placeholder: "\u8BFE\u7A0B ID", min: 1, style: { width: 110 }, value: scopeCourseId, onChange: (v) => setScopeCourseId(v ?? undefined), disabled: batchRunning(batch) }), _jsx(InputNumber, { placeholder: "\u884C\u6570\u4E0A\u9650", min: 1, max: 1000, style: { width: 110 }, value: scopeLimit, onChange: (v) => setScopeLimit(v ?? undefined), disabled: batchRunning(batch) }), _jsx(Popconfirm, { title: "\u786E\u8BA4\u6309\u8303\u56F4\u63D0\u4EA4\u5C0F\u6279\u6B21\uFF1F", description: "\u53EA\u5904\u7406\u6240\u586B\u8BFE\u7A0B\uFF08\u53EF\u518D\u9650\u884C\u6570\u4E0A\u9650\uFF09\u7684\u5F85\u5206\u7C7B\u884C\uFF1B\u8303\u56F4\u5916\u884C\u4E0D\u53D7\u5F71\u54CD\u3002\u6682\u505C\u540E\u7EED\u8DD1\u9700\u643A\u5E26\u76F8\u540C\u8303\u56F4\u3002", onConfirm: doScopeSubmit, disabled: batchRunning(batch) || (scopeCourseId == null && scopeLimit == null), children: _jsx(Button, { disabled: batchRunning(batch) || (scopeCourseId == null && scopeLimit == null), children: "\u8303\u56F4\u63D0\u4EA4\uFF08\u5C0F\u6279\u6B21\uFF09" }) }), _jsx(Popconfirm, { title: "\u786E\u8BA4\u6682\u505C\u5F53\u524D\u6279\u6B21\uFF1F", description: "\u505C\u6B62\u6D3E\u53D1\u65B0\u884C\uFF1B\u5DF2\u6D3E\u53D1\u7684\u884C\u8DD1\u5B8C\u5373\u6682\u505C\uFF08\u7EA6 10 \u79D2\u5185\uFF09\u3002\u7EED\u8DD1\u8BF7\u5E26\u76F8\u540C\u8BFE\u7A0B ID \u548C\u884C\u6570\u4E0A\u9650\u518D\u70B9\u300C\u8303\u56F4\u63D0\u4EA4\uFF08\u5C0F\u6279\u6B21\uFF09\u300D\uFF1B\u70B9\u300C\u63D0\u4EA4 LLM \u5206\u7C7B\u6279\u6B21\u300D\u4F1A\u6309\u5168\u91CF\u63D0\u4EA4\u3002", onConfirm: doPause, disabled: !batchRunning(batch), children: _jsx(Button, { danger: true, disabled: !batchRunning(batch), children: "\u6682\u505C" }) }), _jsx(Popconfirm, { title: "\u786E\u8BA4\u590D\u4F4D\u672A\u8FBE\u6807\u884C\u4EE5\u91CD\u8DD1\uFF1F", description: "\u5C06\u300C\u5B58\u7591/\u672A\u5206\u7C7B\u300D\u884C\u590D\u4F4D\u4E3A\u5F85\u5206\u7C7B\u5E76\u6E05\u7A7A\u4E0A\u4E00\u8F6E\u7ED3\u8BBA\uFF08\u5DF2\u5199\u56DE\u4E0E\u5F85\u5199\u56DE\u884C\u4E0D\u53D7\u5F71\u54CD\uFF09", onConfirm: doResetClassify, disabled: batchRunning(batch), children: _jsx(Button, { disabled: batchRunning(batch), children: "\u590D\u4F4D\u5E76\u91CD\u8DD1\uFF08\u672A\u8FBE\u6807\u884C\uFF09" }) }), _jsx(Button, { onClick: doExportFix, children: "\u5BFC\u51FA\u4FEE\u6B63\u6E05\u5355 xlsx" }), _jsx(Popconfirm, { title: "\u786E\u8BA4\u6267\u884C\u77E5\u8BC6\u4FEE\u6B63\u5199\u56DE\uFF1F", description: "\u4EC5\u6539\u6302\u884C\u66F4\u65B0 knowledge_id\uFF08\u5DF2\u81EA\u52A8\u5907\u4EFD\uFF0C\u53EF\u56DE\u6EDA\uFF09", onConfirm: doApplyFix, disabled: fixApplying || !batch || batch.status !== 'done' || applyCount === 0, children: _jsx(Button, { danger: true, loading: fixApplying, disabled: fixApplying || !batch || batch.status !== 'done' || applyCount === 0, children: "\u6267\u884C\u5199\u56DE\uFF08\u4EC5\u6539\u6302\u884C\uFF09" }) }), _jsx(Typography.Text, { type: "secondary", style: { fontSize: 12 }, children: "\u5B9A\u593A\u4E3A\u6539\u6302\u7684\u884C\u9700\u70B9\u300C\u6267\u884C\u5199\u56DE\u300D\u624D\u4F1A\u5199\u8FDB\u9898\u5E93\uFF08\u81EA\u52A8\u5907\u4EFD\u3001\u53EF\u56DE\u6EDA\uFF09" })] }), batchRunning(batch) && (_jsx(Progress, { percent: batch && batch.total > 0
                                        ? Math.round(((batch.done ?? 0) + (batch.failed ?? 0)) / batch.total * 100)
                                        : 0, status: "active", style: { marginBottom: 12 } })), batch && (_jsxs(_Fragment, { children: [_jsxs(Descriptions, { column: 5, size: "small", style: { marginBottom: 8 }, title: `批次 ${batch.batchNo.slice(-8)}`, children: [_jsx(Descriptions.Item, { label: "\u603B\u6570", children: batch.total }), _jsx(Descriptions.Item, { label: "\u5DF2\u5206\u7C7B\uFF08\u8FDB\u8FC7\u6A21\u578B\uFF09", children: batch.done ?? 0 }), _jsx(Descriptions.Item, { label: "\u672A\u5206\u7C7B", children: batch.unclassified ?? 0 }), _jsx(Descriptions.Item, { label: "\u6539\u6302", children: batch.applyCnt ?? 0 }), _jsx(Descriptions.Item, { label: "\u4FDD\u7559", children: batch.keepCnt ?? 0 }), _jsx(Descriptions.Item, { label: "\u5B58\u7591", children: batch.flagCnt ?? 0 }), _jsx(Descriptions.Item, { label: "\u5931\u8D25\u91CD\u8BD5\u6B21\u6570", children: batch.failed ?? 0 }), _jsx(Descriptions.Item, { label: "Jev \u5206\u7C7B", children: _jsx(Tag, { color: "purple", children: batch.jevClassified ?? 0 }) }), _jsx(Descriptions.Item, { label: "LLM \u515C\u5E95", children: batch.llmFallback ?? 0 }), _jsx(Descriptions.Item, { label: "Jev \u5E73\u5747\u7F6E\u4FE1", children: batch.jevAvgConfidence == null ? '-' : Number(batch.jevAvgConfidence).toFixed(3) }), _jsx(Descriptions.Item, { label: "\u7D2F\u8BA1 LLM \u8C03\u7528", children: batch.usage ? batch.usage.calls.toLocaleString() : '-' }), _jsx(Descriptions.Item, { label: "\u7D2F\u8BA1 tokens", children: batch.usage
                                                        ? `${(batch.usage.totalTokens / 10000).toFixed(1)} 万`
                                                            + `（缓存命中 ${(batch.usage.cachedTokens / 10000).toFixed(1)} 万）`
                                                        : '-' })] }), batch.status === 'failed' && (_jsx(Alert, { type: "error", showIcon: true, style: { marginBottom: 12 }, message: "\u672A\u8FBE\u8D28\u91CF\u95E8\u7981\uFF0C\u7981\u6B62\u5199\u56DE", description: batch.failReason ?? '未分类占比 >5%；可点「复位并重跑」后重新提交' })), batch.status === 'paused' && (_jsx(Alert, { type: "info", showIcon: true, style: { marginBottom: 12 }, message: "\u6279\u6B21\u5DF2\u6682\u505C", description: `已分类 ${batch.done ?? 0}/${batch.total}，剩余 ${batch.unclassified ?? 0} 行保持待分类。续跑请带相同课程 ID 和行数上限点「范围提交（小批次）」；点「提交 LLM 分类批次」将全量提交所有待分类行。` })), batch.status === 'failed' && _jsx("div", { style: { marginBottom: 12 } })] })), _jsxs(Space, { style: { marginBottom: 12 }, wrap: true, children: [['apply', 'keep', 'flag', 'pending'].map((st) => (_jsx(Button, { size: "small", type: fixStatus === st ? 'primary' : 'default', onClick: () => { setFixStatus(st); setFixPage(1); setSelectedRowKeys([]); }, children: STATUS_TAG[st]?.text ?? st }, st))), _jsx(Button, { size: "small", onClick: () => { setFixStatus(undefined); setFixPage(1); setSelectedRowKeys([]); }, children: "\u5168\u90E8" })] }), fixStatus === 'flag' && selectedRowKeys.length > 0 && (_jsxs(Space, { style: { marginBottom: 8 }, wrap: true, children: [_jsxs(Typography.Text, { children: ["\u5DF2\u9009 ", selectedRowKeys.length, " \u884C"] }), _jsx(Popconfirm, { title: "\u6279\u91CF\u5B9A\u593A\u4E3A\u6539\u6302\uFF1F", description: "\u5404\u884C\u91C7\u7528\u6A21\u578B\u5EFA\u8BAE\u7684\u65B0\u77E5\u8BC6\u70B9\uFF1B\u5B9A\u593A\u540E\u9700\u70B9\u300C\u6267\u884C\u5199\u56DE\u300D\u624D\u5199\u8FDB\u9898\u5E93", onConfirm: () => doBatchAdjudicate('apply'), children: _jsx(Button, { type: "primary", size: "small", children: "\u6279\u91CF\u5B9A\u593A\u4E3A\u6539\u6302" }) }), _jsx(Popconfirm, { title: "\u6279\u91CF\u5B9A\u593A\u4E3A\u4FDD\u7559\uFF1F", description: "\u5404\u884C\u7EF4\u6301\u539F\u77E5\u8BC6\u70B9\uFF08\u539F\u8282\u70B9\u5DF2\u5220\u7684\u884C\u4F1A\u88AB\u8DF3\u8FC7\uFF09", onConfirm: () => doBatchAdjudicate('keep'), children: _jsx(Button, { size: "small", children: "\u6279\u91CF\u5B9A\u593A\u4E3A\u4FDD\u7559" }) }), _jsx(Button, { size: "small", onClick: () => setSelectedRowKeys([]), children: "\u6E05\u7A7A\u9009\u62E9" })] })), _jsx(Table, { rowKey: "rowId", size: "small", loading: fixQuery.isLoading, dataSource: fixQuery.data?.items ?? [], columns: fixColumns, rowSelection: fixStatus === 'flag' ? {
                                        selectedRowKeys,
                                        onChange: (keys) => setSelectedRowKeys(keys),
                                    } : undefined, pagination: {
                                        current: fixPage, pageSize: fixPageSize, total: fixQuery.data?.total ?? 0,
                                        showSizeChanger: true,
                                        onChange: (p, ps) => {
                                            if (ps !== fixPageSize) {
                                                setFixPageSize(ps);
                                                setFixPage(1);
                                            }
                                            else {
                                                setFixPage(p);
                                            }
                                        },
                                    } })] })),
                    },
                    {
                        key: 'report', label: '质量报告',
                        children: report ? (_jsxs(Descriptions, { column: 2, bordered: true, size: "small", title: `批次 ${report.batchNo.slice(-8)} 质量报告`, children: [_jsx(Descriptions.Item, { label: "\u603B\u884C\u6570", children: report.total }), _jsx(Descriptions.Item, { label: "\u72B6\u6001", children: report.status }), _jsx(Descriptions.Item, { label: "\u6539\u6302\uFF08\u5DF2\u6309\u65B0\u77E5\u8BC6\u70B9\u4FEE\u6B63\uFF09", children: report.applyCnt ?? 0 }), _jsx(Descriptions.Item, { label: "\u4FDD\u7559\uFF08\u5224\u5B9A\u5F52\u5C5E\u6B63\u786E\u4FDD\u7559\uFF09", children: report.keepCnt ?? 0 }), _jsx(Descriptions.Item, { label: "\u5B58\u7591\uFF08\u7F6E\u4FE1\u4E0D\u8DB3\u4FDD\u6301\u539F\u8282\u70B9\uFF09", children: report.flagCnt ?? 0 }), _jsx(Descriptions.Item, { label: "\u5931\u8D25\u91CD\u8BD5\u6B21\u6570", children: report.failed ?? 0 }), _jsx(Descriptions.Item, { label: "\u672A\u5206\u7C7B\uFF08\u672A\u8FDB\u6A21\u578B\uFF09", children: report.unclassified ?? 0 }), _jsx(Descriptions.Item, { label: "\u81EA\u4E00\u81F4\u7387\uFF08\u4E09\u91C7\u6837\u5168\u4E00\u81F4\u5360\u6BD4\uFF09", children: report.selfConsistencyRate == null ? '-' : `${(report.selfConsistencyRate * 100).toFixed(1)}%` })] })) : (_jsx(Alert, { type: "info", showIcon: true, message: "\u5C1A\u65E0\u6279\u6B21\u6570\u636E\uFF1A\u63D0\u4EA4\u5206\u7C7B\u6279\u6B21\u540E\u6B64\u5904\u8F93\u51FA\u8D28\u91CF\u62A5\u544A\u3002" })),
                    },
                ] })] }));
};
export default KnowledgeFixPage;

import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
/**
 * 生成工作台：发起区 + 批次总览表（进行中置顶/历史分页）+ 草稿审核面板。
 * 轮询与完成联动集中在本组件（页签容器加 forceRender，停在其他页签也能收到完成提示）。
 */
import React, { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Descriptions, message } from 'antd';
import { LoadingOutlined } from '@ant-design/icons';
import { getGenProgress, listGenBatches, pauseGenBatch, resumeGenBatch, retryGenBatch, } from '../theoryGenApi';
import GenStartBar from './GenStartBar';
import GenBatchOverviewTable from './GenBatchOverviewTable';
import DraftReviewPanel from './DraftReviewPanel';
import { usePublishExport } from './PublishExportActions';
import { genBatchOptionLabel } from './constants';
import { usePaneActive } from '../../../layouts/TabKeepAlive';
import './GenWorkbench.css';
const GenWorkbenchPage = ({ workTypeOptions, preset }) => {
    const qc = useQueryClient();
    const paneActive = usePaneActive(); // 标签页不可见时暂停轮询（§3.6）
    const [activeBatchId, setActiveBatchId] = useState();
    const [historyPage, setHistoryPage] = useState(1);
    const [historyPageSize, setHistoryPageSize] = useState(10);
    const [panelOpen, setPanelOpen] = useState(false);
    const [draftBatchId, setDraftBatchId] = useState();
    const [flashBatchId, setFlashBatchId] = useState();
    const panelRef = React.useRef(null);
    /** 打开草稿面板并滚动到可视位置（面板在长页面底部，不滚动看起来像"没反应"） */
    const openDraftPanel = (batchId) => {
        setDraftBatchId(batchId);
        setPanelOpen(true);
        window.setTimeout(() => {
            panelRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }, 0);
    };
    // 进行中区：状态过滤 + 仅在有进行中批次时 3s 轮询（历史分页不随轮询重拉）
    const activeQuery = useQuery({
        queryKey: ['theorygen-batches', 'active'],
        queryFn: () => listGenBatches({ status: 'pending,running', page: 1, pageSize: 50 }),
        refetchInterval: (q) => (paneActive && (q.state.data?.items.length ?? 0) > 0 ? 3000 : false),
    });
    const historyQuery = useQuery({
        queryKey: ['theorygen-batches', 'history', historyPage, historyPageSize],
        // 终态批次服务端过滤：与进行中区互斥，分页总数/页码一致（客户端剔除会破坏分页）
        queryFn: () => listGenBatches({ status: 'done,failed,paused', page: historyPage, pageSize: historyPageSize }),
    });
    const activeItems = activeQuery.data?.items ?? [];
    const historyItems = historyQuery.data?.items ?? [];
    // 接管批次的进度轮询（终态停止）
    const progressQuery = useQuery({
        queryKey: ['theorygen-progress', activeBatchId],
        queryFn: () => getGenProgress(activeBatchId),
        enabled: !!activeBatchId,
        refetchInterval: (q) => {
            if (!paneActive)
                return false;
            const s = q.state.data?.status;
            return s === 'running' || s === 'pending' ? 3000 : false;
        },
    });
    // 自动接管：当前批次不在进行中列表（完成/终止）时切换到新的进行中批次
    React.useEffect(() => {
        const running = activeItems.find((b) => b.status === 'running' || b.status === 'pending');
        if (!running) {
            return;
        }
        if (!activeBatchId || !activeItems.some((b) => b.id === activeBatchId)) {
            setActiveBatchId(running.id);
        }
    }, [activeBatchId, activeQuery.data]);
    // 终态联动：切换接管目标时重置跃迁基线；状态以进度接口（按 id 直查，最新）为准，
    // 进行中/历史列表兜底——任一来源先看到终态都能立即联动
    const prevStatusRef = React.useRef();
    React.useEffect(() => {
        prevStatusRef.current = undefined;
    }, [activeBatchId]);
    React.useEffect(() => {
        if (!activeBatchId) {
            return;
        }
        const activeRow = activeItems.find((b) => b.id === activeBatchId);
        const histRow = (historyQuery.data?.items ?? []).find((b) => b.id === activeBatchId);
        const status = progressQuery.data?.status ?? activeRow?.status ?? histRow?.status;
        if (!status) {
            return;
        }
        const prev = prevStatusRef.current;
        prevStatusRef.current = status;
        if ((prev === 'running' || prev === 'pending') && status === 'done') {
            message.success('生成批次已完成，已为您载入试题草稿');
            setFlashBatchId(activeBatchId);
            window.setTimeout(() => setFlashBatchId(undefined), 2600);
            // 草稿面板空闲或正锁定该批次 → 自动载入；正在审别的批次则不打断
            if (!draftBatchId || draftBatchId === activeBatchId) {
                setDraftBatchId(activeBatchId);
                setPanelOpen(true);
            }
            qc.invalidateQueries({ queryKey: ['theorygen-batches'] });
        }
        else if ((prev === 'running' || prev === 'pending') && status === 'failed') {
            qc.invalidateQueries({ queryKey: ['theorygen-batches'] });
            message.warning({
                content: `生成批次失败：${activeRow?.failReason ?? progressQuery.data?.failReason ?? '未知原因'}`,
                duration: 6,
            });
        }
    }, [activeBatchId, activeItems, historyQuery.data, progressQuery.data?.status,
        progressQuery.data?.failReason, draftBatchId, qc]);
    const batchById = new Map();
    [...activeItems, ...(historyQuery.data?.items ?? [])].forEach((b) => batchById.set(b.id, b));
    const workTypeName = (id) => (id == null ? '-' : workTypeOptions.find((o) => o.value === id)?.label ?? `工种${id}`);
    const batchLabel = (id) => {
        const b = batchById.get(id);
        return b ? genBatchOptionLabel(b) : `批次…${id.slice(-8)}`;
    };
    const publishExport = usePublishExport(() => {
        qc.invalidateQueries({ queryKey: ['theorygen-batches'] });
    });
    const onAction = async (action, b) => {
        try {
            switch (action) {
                case 'pause':
                    await pauseGenBatch(b.id);
                    break;
                case 'resume':
                    await resumeGenBatch(b.id);
                    setActiveBatchId(b.id);
                    break;
                case 'retry':
                    await retryGenBatch(b.id);
                    setActiveBatchId(b.id);
                    break;
                case 'publish':
                    publishExport.openPublish(b.id);
                    return;
                case 'export':
                    void publishExport.exportXlsx(b.id);
                    return;
                default:
                    return;
            }
            qc.invalidateQueries({ queryKey: ['theorygen-batches'] });
            qc.invalidateQueries({ queryKey: ['theorygen-progress'] });
        }
        catch {
            // 错误提示由拦截器统一处理
        }
    };
    const progress = progressQuery.data;
    const live = progress && (progress.status === 'running' || progress.status === 'pending');
    return (_jsxs("div", { className: "gen-workbench", children: [_jsx(GenStartBar, { workTypeOptions: workTypeOptions, preset: preset, onSubmitted: (id) => {
                    setActiveBatchId(id);
                    qc.invalidateQueries({ queryKey: ['theorygen-batches'] });
                } }), live && (_jsxs(Descriptions, { size: "small", column: 4, title: _jsxs("span", { children: [_jsx(LoadingOutlined, { style: { color: '#1677ff', marginRight: 6 } }), `批次 …${progress.id.slice(-8)} 进行中`] }), children: [_jsx(Descriptions.Item, { label: "\u5DF2\u5B8C\u6210\u70B9", children: progress.donePoints }), _jsx(Descriptions.Item, { label: "\u5931\u8D25\u70B9", children: progress.failedPoints }), _jsx(Descriptions.Item, { label: "\u5DF2\u4EA7\u9898", children: progress.totalQuestions }), _jsx(Descriptions.Item, { label: "\u603B\u70B9\u6570", children: progress.totalPoints })] })), _jsx("div", { children: _jsx(Button, { size: "small", style: { marginBottom: 8 }, onClick: () => openDraftPanel(undefined), children: "\u8349\u7A3F\u5BA1\u6838\uFF08\u5168\u90E8\u6279\u6B21\uFF09" }) }), _jsx(GenBatchOverviewTable, { activeItems: activeItems, historyItems: historyItems, historyLoading: historyQuery.isLoading, historyPage: historyPage, historyPageSize: historyPageSize, historyTotal: historyQuery.data?.total ?? 0, onHistoryPageChange: (p, s) => {
                    setHistoryPage(s !== historyPageSize ? 1 : p);
                    setHistoryPageSize(s);
                }, workTypeName: workTypeName, flashBatchId: flashBatchId, publishingBatchId: publishExport.publishingBatchId, exportingBatchId: publishExport.exportingBatchId, onOpenDraft: (b) => openDraftPanel(b.id), onAction: onAction }), _jsx("div", { ref: panelRef, children: panelOpen && (_jsx(DraftReviewPanel, { lockedBatch: draftBatchId ? batchById.get(draftBatchId) : undefined, defaultAllMode: !draftBatchId, batchLabel: batchLabel, onClose: () => setPanelOpen(false), onChanged: () => qc.invalidateQueries({ queryKey: ['theorygen-batches'] }) })) }), publishExport.modal] }));
};
export default GenWorkbenchPage;

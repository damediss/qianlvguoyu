import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useMemo, useState } from 'react';
import { Button, Card, Descriptions, Drawer, Modal, Progress, Space, Tag, Timeline, Typography, message, } from 'antd';
import { ArrowLeftOutlined, FileDoneOutlined, FileAddOutlined, FolderOpenOutlined, HistoryOutlined, RollbackOutlined, SyncOutlined, ThunderboltOutlined, } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../../api/training';
import { downloadUrlFile } from '../../../api/files';
import { TrainingProcessSteps } from '../TrainingProcessSteps';
import { ExportDockBar } from './components/ExportDockBar';
import { FillFromTemplateModal } from './components/FillFromTemplateModal';
import { MaterialTreePanel } from './components/MaterialTreePanel';
import { MaterialWorkspacePanel } from './components/MaterialWorkspacePanel';
import { SubsidyMaterialTemplateLibraryModal } from './components/SubsidyMaterialTemplateLibraryModal';
import '../TrainingSubsidyDeclarePage.css';
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
function phaseLabel(v) {
    return PHASE_OPTS.find((p) => p.value === v)?.label || v || '—';
}
function trainModeLabel(v) {
    if (v === 'skill_upgrade')
        return '岗位技能提升';
    if (v === 'apprentice')
        return '新型学徒';
    return v || '—';
}
function money(v) {
    if (v == null)
        return '—';
    return `¥${Number(v).toLocaleString('zh-CN', { maximumFractionDigits: 2 })}`;
}
export function SubsidyClassWorkbench({ subsidy, onBack, onSubsidyChange }) {
    const qc = useQueryClient();
    const detail = subsidy;
    const [selectedInstanceId, setSelectedInstanceId] = useState(null);
    const [exportSelectedIds, setExportSelectedIds] = useState(new Set());
    const [eventsOpen, setEventsOpen] = useState(false);
    const [replacingId, setReplacingId] = useState(null);
    const [deletingId, setDeletingId] = useState(null);
    const [templateLibOpen, setTemplateLibOpen] = useState(false);
    const [fillOpen, setFillOpen] = useState(false);
    const [fillForInstance, setFillForInstance] = useState(false);
    const instancesQ = useQuery({
        queryKey: ['subsidy-material-instances', detail.id],
        queryFn: () => trainApi.listSubsidyMaterialInstances(detail.id),
        staleTime: 30_000,
    });
    const eventsQ = useQuery({
        queryKey: ['subsidy-events', detail.id],
        queryFn: () => trainApi.listSubsidyEvents(detail.id),
        enabled: eventsOpen,
    });
    const classMatsQ = useQuery({
        queryKey: ['subsidy-class-materials', detail.id],
        queryFn: () => trainApi.getSubsidyClassMaterials(detail.id),
        staleTime: 30_000,
    });
    const invalidate = () => {
        void qc.invalidateQueries({ queryKey: ['subsidies'] });
        void qc.invalidateQueries({ queryKey: ['subsidy-material-instances', detail.id] });
        void qc.invalidateQueries({ queryKey: ['subsidy-class-materials', detail.id] });
        void qc.invalidateQueries({ queryKey: ['subsidy-events', detail.id] });
    };
    const syncMu = useMutation({
        mutationFn: () => trainApi.syncSubsidyClassMaterials(detail.id),
        onSuccess: (res) => {
            message.success(res.message || '已从本班刷新材料');
            invalidate();
        },
        onError: (e) => message.error(e.message || '刷新失败'),
    });
    const claimFormsMu = useMutation({
        mutationFn: () => trainApi.generateSubsidyClaimForms(detail.id),
        onSuccess: async (res) => {
            message.success(res.message || '已生成个人申领表');
            invalidate();
            // 私有 COS 无签名直链会 403，必须走鉴权的 /api/files/{id}/download（filePath）
            if (res.filePath || res.url) {
                try {
                    await downloadUrlFile(res.filePath || res.url, res.downloadFileName || `个人申领表_${detail.id}.zip`);
                }
                catch {
                    message.info('申领表已生成，可在材料清单中导出下载');
                }
            }
            else if ((res.claimFormsGenerated ?? 0) > 0) {
                message.info('申领表已入库，打包下载未返回，请用下方导出');
            }
        },
        onError: (e) => message.error(e.message || '生成申领表失败'),
    });
    const transitionMu = useMutation({
        mutationFn: (p) => trainApi.transitionSubsidy(detail.id, p.toPhase, p.remark),
        onSuccess: (row) => {
            message.success('状态已更新');
            onSubsidyChange(row);
            invalidate();
        },
        onError: (e) => message.error(e.message || '流转失败'),
    });
    const exportMu = useMutation({
        mutationFn: (ids) => trainApi.exportSubsidyMaterials(detail.id, ids),
        onSuccess: async (res) => {
            if (!res.ok) {
                message.warning(res.message || '导出未完成');
                return;
            }
            message.success(res.message || '已导出');
            if (res.skipped?.length) {
                message.info(`跳过 ${res.skipped.length} 项无文件材料`);
            }
            invalidate();
            const fresh = await trainApi.getSubsidy(detail.id);
            onSubsidyChange(fresh);
            // 私有 COS 无签名直链会 403，必须走鉴权的 /api/files/{id}/download（filePath）
            if (res.filePath || res.url) {
                try {
                    await downloadUrlFile(res.filePath || res.url, `补贴材料_${detail.id}.zip`);
                }
                catch {
                    message.info('材料已生成，可稍后下载');
                }
            }
        },
        onError: (e) => message.error(e.message || '导出失败'),
    });
    const genMu = useMutation({
        mutationFn: () => trainApi.fillAllSubsidyMaterials(detail.id),
        onSuccess: (res) => {
            if (res.failed > 0) {
                message.warning(res.message || `部分失败：成功 ${res.filled}，失败 ${res.failed}`);
            }
            else {
                message.success(res.message || `已按清单填数 ${res.filled} 项`);
            }
            invalidate();
        },
        onError: (e) => message.error(e.message || '一键填数失败'),
    });
    const confirmMu = useMutation({
        mutationFn: (instanceId) => trainApi.confirmSubsidyMaterialInstance(instanceId),
        onSuccess: () => {
            message.success('已确认');
            invalidate();
        },
        onError: (e) => message.error(e.message || '确认失败'),
    });
    const groups = instancesQ.data?.groups ?? [];
    const items = instancesQ.data?.items ?? [];
    const selectedItem = useMemo(() => {
        if (!selectedInstanceId)
            return null;
        return items.find((i) => String(i.id) === selectedInstanceId) ?? null;
    }, [items, selectedInstanceId]);
    const handleReplaceFile = async (instanceId, file) => {
        setReplacingId(instanceId);
        try {
            await trainApi.replaceSubsidyMaterialFile(instanceId, file);
            message.success('已保存');
            setSelectedInstanceId(instanceId);
            invalidate();
        }
        catch (e) {
            message.error(e.message || '上传失败');
            throw e;
        }
        finally {
            setReplacingId(null);
        }
    };
    const handleDeleteInstance = async (instanceId) => {
        setDeletingId(instanceId);
        try {
            await trainApi.deleteSubsidyMaterialInstance(instanceId);
            if (selectedInstanceId === instanceId)
                setSelectedInstanceId(null);
            setExportSelectedIds((prev) => {
                const next = new Set(prev);
                next.delete(instanceId);
                return next;
            });
            message.success('已删除材料');
            invalidate();
        }
        catch (e) {
            message.error(e.message || '删除材料失败');
        }
        finally {
            setDeletingId(null);
        }
    };
    const toggleExport = (id, checked) => {
        setExportSelectedIds((prev) => {
            const next = new Set(prev);
            if (checked)
                next.add(id);
            else
                next.delete(id);
            return next;
        });
    };
    const toggleAllExport = (checked, ids) => {
        setExportSelectedIds(checked ? new Set(ids) : new Set());
    };
    const packProgress = instancesQ.data?.requiredTotal
        ? Math.round(((instancesQ.data.confirmedRequired ?? 0) / instancesQ.data.requiredTotal) * 100)
        : 0;
    return (_jsxs("div", { className: "training-page training-subsidy-declare-page subsidy-class-workbench", children: [_jsxs("div", { className: "subsidy-workbench-header", children: [_jsxs(Space, { wrap: true, children: [_jsx(Button, { icon: _jsx(ArrowLeftOutlined, {}), onClick: onBack, children: "\u8FD4\u56DE\u73ED\u7EA7\u5217\u8868" }), _jsx(Typography.Title, { level: 4, style: { margin: 0 }, children: detail.className || `班级 #${detail.classId}` }), _jsx(Tag, { color: PHASE_COLOR[detail.phase || ''] || 'default', children: phaseLabel(detail.phase) }), detail.profileLocked ? _jsx(Tag, { color: "orange", children: "\u914D\u65B9\u5DF2\u9501\u5B9A" }) : _jsx(Tag, { children: "\u914D\u65B9\u672A\u9501\u5B9A" }), _jsx(Tag, { color: "processing", children: "\u6750\u6599\u5DE5\u4F5C\u53F0" })] }), _jsx(Typography.Text, { type: "secondary", children: [detail.regionName || detail.regionCode, trainModeLabel(detail.trainMode), detail.policyYear ? `${detail.policyYear}年` : null]
                            .filter(Boolean)
                            .join(' · ') })] }), _jsx(TrainingProcessSteps, { current: 8 }), _jsxs(Card, { size: "small", style: { marginBottom: 12 }, children: [_jsxs(Space, { wrap: true, children: [_jsx(Button, { icon: _jsx(SyncOutlined, {}), loading: syncMu.isPending, onClick: () => syncMu.mutate(), children: "\u4ECE\u672C\u73ED\u5237\u65B0\u6750\u6599" }), _jsx(Button, { type: "primary", icon: _jsx(FileDoneOutlined, {}), loading: claimFormsMu.isPending, title: "\u4EC5\u751F\u6210\u5DF2\u83B7\u8BC1\u5B66\u5458\u7684\u4E2A\u4EBA\u7533\u9886\u8868", onClick: () => claimFormsMu.mutate(), children: "\u4E00\u952E\u751F\u6210\u4E2A\u4EBA\u7533\u9886\u8868" }), _jsx(Button, { icon: _jsx(FolderOpenOutlined, {}), onClick: () => setTemplateLibOpen(true), children: "\u6A21\u677F\u5E93" }), _jsx(Button, { type: "primary", ghost: true, icon: _jsx(FileAddOutlined, {}), onClick: () => {
                                    setFillForInstance(false);
                                    setFillOpen(true);
                                }, children: "\u4ECE\u6A21\u677F\u751F\u6210" }), _jsx(Button, { icon: _jsx(HistoryOutlined, {}), onClick: () => setEventsOpen(true), children: "\u65F6\u95F4\u7EBF" }), detail.phase === 'draft' && (_jsx(Button, { loading: transitionMu.isPending, onClick: () => transitionMu.mutate({ toPhase: 'checking' }), children: "\u8FDB\u5165\u6838\u9A8C" })), detail.phase === 'rejected' && (_jsx(Button, { loading: transitionMu.isPending, onClick: () => transitionMu.mutate({ toPhase: 'draft', remark: '退回后回到草稿' }), children: "\u56DE\u5230\u8349\u7A3F" })), (detail.phase === 'draft' ||
                                detail.phase === 'checking' ||
                                detail.phase === 'ready' ||
                                detail.phase === 'rejected') && (_jsx(Button, { loading: transitionMu.isPending, onClick: () => transitionMu.mutate({ toPhase: 'submitted', remark: '提交人社' }), children: "\u63D0\u4EA4\u4EBA\u793E" })), detail.phase === 'submitted' && (_jsx(Button, { danger: true, icon: _jsx(RollbackOutlined, {}), loading: transitionMu.isPending, onClick: () => {
                                    Modal.confirm({
                                        title: '退回修改',
                                        content: '退回后申报进入「已退回」状态，可重新编辑、填数和替换材料。确认退回？',
                                        okText: '确认退回',
                                        cancelText: '取消',
                                        onOk: () => transitionMu.mutateAsync({ toPhase: 'rejected', remark: '退回修改' }),
                                    });
                                }, children: "\u9000\u56DE\u4FEE\u6539" })), _jsx(Button, { icon: _jsx(ThunderboltOutlined, {}), type: "primary", ghost: true, loading: genMu.isPending, onClick: () => genMu.mutate(), children: "\u4E00\u952E\u586B\u6570\u5168\u90E8\u6750\u6599" })] }), _jsx("div", { style: { marginTop: 10 }, children: _jsxs(Typography.Text, { type: "secondary", style: { fontSize: 12 }, children: ["\u5DF2\u83B7\u8BC1\u7533\u62A5 ", classMatsQ.data?.studentCount ?? '—', " \u4EBA", classMatsQ.data?.classStudentTotal != null
                                    ? `（班级共 ${classMatsQ.data.classStudentTotal} 人）`
                                    : '', ' ', "\u00B7 \u8BC1\u4E66 ", classMatsQ.data?.certificateWithFile ?? 0, "/", classMatsQ.data?.certificateTotal ?? 0, " \u00B7 \u7167\u7247", ' ', classMatsQ.data?.photoPackReady ? '已就绪' : '未生成'] }) })] }), _jsxs("div", { className: "subsidy-material-kit-layout", children: [_jsx(Card, { size: "small", className: "subsidy-material-kit-left", title: "\u672C\u73ED\u6750\u6599\u6E05\u5355", loading: instancesQ.isLoading, children: _jsx(MaterialTreePanel, { groups: groups, selectedInstanceId: selectedInstanceId, exportSelectedIds: exportSelectedIds, onSelectInstance: setSelectedInstanceId, onToggleExport: toggleExport, onToggleAllExport: toggleAllExport, onReplaceFile: handleReplaceFile, onDeleteInstance: handleDeleteInstance, replaceLoading: replacingId != null, deleteLoadingId: deletingId }) }), _jsx(Card, { size: "small", className: "subsidy-material-kit-right", title: "\u9884\u89C8\u4E0E\u64CD\u4F5C", children: _jsx(MaterialWorkspacePanel, { item: selectedItem, confirmLoading: confirmMu.isPending, replaceLoading: replacingId != null && replacingId === selectedInstanceId, onReplaceFile: selectedInstanceId
                                ? (file) => handleReplaceFile(selectedInstanceId, file)
                                : undefined, onConfirm: selectedInstanceId
                                ? () => confirmMu.mutate(selectedInstanceId)
                                : undefined, onFillFromTemplate: selectedInstanceId
                                ? () => {
                                    setFillForInstance(true);
                                    setFillOpen(true);
                                }
                                : undefined, onOfficeSaved: () => {
                                void qc.invalidateQueries({ queryKey: ['subsidy-material-instances', detail.id] });
                            } }) })] }), _jsx(Card, { size: "small", style: { marginTop: 12 }, children: _jsxs(Descriptions, { size: "small", column: 4, children: [_jsxs(Descriptions.Item, { label: "\u5DF2\u786E\u8BA4\u5FC5\u586B", children: [instancesQ.data?.confirmedRequired ?? 0, "/", instancesQ.data?.requiredTotal ?? '—'] }), _jsxs(Descriptions.Item, { label: "\u6709\u6587\u4EF6", children: [instancesQ.data?.hasFileCount ?? 0, "/", instancesQ.data?.total ?? '—'] }), _jsx(Descriptions.Item, { label: "\u6750\u6599\u5305", children: detail.materialPackagePath ? _jsx(Tag, { color: "success", children: "\u5DF2\u751F\u6210" }) : _jsx(Tag, { children: "\u672A\u751F\u6210" }) }), _jsx(Descriptions.Item, { label: "\u8FDB\u5EA6", children: _jsx(Progress, { percent: packProgress, size: "small", style: { maxWidth: 160, marginBottom: 0 } }) })] }) }), _jsx(ExportDockBar, { total: items.length, selectedCount: exportSelectedIds.size, exporting: exportMu.isPending, lastPackagePath: detail.materialPackagePath, onExport: () => exportMu.mutate([...exportSelectedIds]), onDownloadLast: detail.materialPackagePath
                    ? () => {
                        void downloadUrlFile(detail.materialPackagePath, `补贴材料_${detail.id}.zip`).catch(() => message.error('下载失败'));
                    }
                    : undefined }), _jsx(SubsidyMaterialTemplateLibraryModal, { open: templateLibOpen, onClose: () => setTemplateLibOpen(false), classId: detail.classId, regionCode: detail.regionCode, trainMode: detail.trainMode, policyYear: detail.policyYear }), _jsx(FillFromTemplateModal, { open: fillOpen, onClose: () => setFillOpen(false), subsidyId: detail.id, classId: detail.classId, instanceId: fillForInstance ? selectedInstanceId : null, instanceTitle: fillForInstance ? selectedItem?.title : null, instanceHasFile: fillForInstance ? !!selectedItem?.hasFile : false, instanceFileName: fillForInstance ? selectedItem?.fileName : null, hasUploadTemplate: fillForInstance ? !!selectedItem?.hasUploadTemplate : false, uploadTemplateFileName: fillForInstance ? selectedItem?.uploadTemplateFileName : null, materialType: fillForInstance ? selectedItem?.materialType : null, onFilled: (instanceId) => {
                    invalidate();
                    if (instanceId != null)
                        setSelectedInstanceId(String(instanceId));
                } }), _jsx(Drawer, { title: "\u8865\u8D34\u5355\u65F6\u95F4\u7EBF", width: 480, open: eventsOpen, onClose: () => setEventsOpen(false), children: (eventsQ.data?.length ?? 0) === 0 ? (_jsx(Typography.Text, { type: "secondary", children: "\u6682\u65E0\u4E8B\u4EF6" })) : (_jsx(Timeline, { items: (eventsQ.data || []).map((e) => ({
                        children: (_jsxs("div", { children: [_jsxs(Space, { size: 8, wrap: true, children: [_jsx(Tag, { children: e.action }), e.fromPhase || e.toPhase ? (_jsxs("span", { children: [phaseLabel(e.fromPhase), " \u2192 ", phaseLabel(e.toPhase)] })) : null] }), e.remark ? _jsx("div", { style: { marginTop: 4, fontSize: 13 }, children: e.remark }) : null, _jsx("div", { style: { color: '#8c8c8c', fontSize: 12 }, children: e.createdAt })] })),
                    })) })) })] }));
}

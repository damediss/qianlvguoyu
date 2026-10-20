import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useCallback, useEffect, useState } from 'react';
import { Alert, Button, Card, Descriptions, Drawer, Input, Modal, Space, Table, Tabs, Tag, Timeline, Typography, Upload, message, } from 'antd';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { CheckOutlined, CloseOutlined, CloudDownloadOutlined, CloudUploadOutlined, EyeOutlined, FileTextOutlined, } from '@ant-design/icons';
import * as trainApi from '../../api/training';
import { FilePreviewModal } from '../../components/FilePreviewModal';
const { Paragraph } = Typography;
const STATUS_MAP = {
    3: { label: '待企业审核', color: 'warning' },
    4: { label: '企业已确认', color: 'success' },
    5: { label: '已驳回', color: 'error' },
};
function formatTime(value) {
    if (!value)
        return '-';
    return value.replace('T', ' ').slice(0, 19);
}
function tabStatus(tab) {
    if (tab === 'pending')
        return 3;
    if (tab === 'confirmed')
        return 4;
    if (tab === 'rejected')
        return 5;
    return undefined;
}
function fileExtFromName(fileName) {
    if (!fileName?.includes('.'))
        return undefined;
    return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
}
function renderRejectHistory(items) {
    if (!items || items.length === 0) {
        return _jsx("span", { style: { color: '#999' }, children: "\u6682\u65E0\u9A73\u56DE\u5386\u53F2" });
    }
    return (_jsx(Timeline, { items: items
            .slice()
            .reverse()
            .map((item, idx) => ({
            key: idx,
            color: 'red',
            children: (_jsxs("div", { children: [_jsx("div", { style: { fontWeight: 600 }, children: item.reason || '（未填写理由）' }), _jsxs("div", { style: { fontSize: 12, color: '#999' }, children: [item.rejectedByUsername || '企业', item.rejectedAt ? ` · ${formatTime(item.rejectedAt)}` : ''] })] })),
        })) }));
}
function isReturnReady(plan) {
    if (!plan)
        return false;
    return !!plan.enterpriseRosterFileId && !!plan.enterpriseShiftFileId;
}
function returnMissingHint(plan) {
    if (!plan)
        return '尚未上传筛选名单与排班表';
    const miss = [];
    if (!plan.enterpriseRosterFileId)
        miss.push('筛选后培训名单');
    if (!plan.enterpriseShiftFileId)
        miss.push('排班表');
    if (miss.length === 0)
        return '';
    return `还缺：${miss.join('、')}`;
}
function returnReadyLabel(plan) {
    const ready = isReturnReady(plan);
    if (plan.status === 4) {
        return ready
            ? { text: '已确认 · 材料已齐', color: 'success' }
            : { text: '已确认 · 待补交', color: 'warning' };
    }
    if (ready)
        return { text: '材料已齐', color: 'processing' };
    if (plan.enterpriseRosterFileId || plan.enterpriseShiftFileId) {
        return { text: '部分回传', color: 'warning' };
    }
    return { text: '未回传', color: 'default' };
}
export function EnterprisePlanReviewPanel() {
    const queryClient = useQueryClient();
    const [activeTab, setActiveTab] = useState('pending');
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(10);
    const [titleFilter, setTitleFilter] = useState('');
    const [detailOpen, setDetailOpen] = useState(false);
    const [currentPlan, setCurrentPlan] = useState(null);
    const [actionLoading, setActionLoading] = useState(false);
    const [rejectOpen, setRejectOpen] = useState(false);
    const [rejectReason, setRejectReason] = useState('');
    const [previewTarget, setPreviewTarget] = useState(null);
    const listQuery = useQuery({
        queryKey: ['enterprise-plan-review', activeTab, page, pageSize, titleFilter],
        queryFn: () => trainApi.pageThreeYearPlans({
            page,
            pageSize,
            status: tabStatus(activeTab),
            title: titleFilter || undefined,
        }),
    });
    const planDetailQuery = useQuery({
        queryKey: ['enterprise-plan-review-detail', currentPlan?.id],
        queryFn: () => trainApi.getThreeYearPlan(currentPlan.id),
        enabled: detailOpen && !!currentPlan?.id,
        staleTime: 30_000,
    });
    const plan = planDetailQuery.data ?? currentPlan;
    const refreshList = useCallback(() => {
        queryClient.invalidateQueries({ queryKey: ['enterprise-plan-review'] });
    }, [queryClient]);
    const openDetail = (row) => {
        setCurrentPlan(row);
        setDetailOpen(true);
    };
    const closeDetail = () => {
        setDetailOpen(false);
        setCurrentPlan(null);
        setPreviewTarget(null);
    };
    const openMaterialPreview = (planId, kind, fileName, fallbackLabel) => {
        setPreviewTarget({
            fileUrl: trainApi.threeYearPlanMaterialPreviewUrl(planId, kind),
            fileName: fileName || fallbackLabel,
            fileType: fileExtFromName(fileName),
        });
    };
    const invalidateDetail = (planId) => {
        queryClient.invalidateQueries({ queryKey: ['enterprise-plan-review-detail', planId] });
    };
    const runAction = async (fn, successMsg) => {
        setActionLoading(true);
        try {
            await fn();
            message.success(successMsg);
            refreshList();
            if (currentPlan?.id) {
                invalidateDetail(currentPlan.id);
            }
        }
        catch (e) {
            message.error(e instanceof Error ? e.message : '操作失败');
        }
        finally {
            setActionLoading(false);
        }
    };
    useEffect(() => {
        const latest = planDetailQuery.data;
        if (latest) {
            setCurrentPlan(latest);
        }
    }, [planDetailQuery.data]);
    const rawItems = listQuery.data?.items ?? [];
    const list = activeTab === 'all'
        ? rawItems.filter((p) => Number(p.status) >= 3)
        : rawItems;
    const columns = [
        { title: '规划编号', dataIndex: 'planNo', width: 180 },
        { title: '方案标题', dataIndex: 'title', ellipsis: true },
        {
            title: '状态',
            dataIndex: 'status',
            width: 120,
            render: (status, row) => {
                const meta = STATUS_MAP[status] ?? { label: row.statusLabel ?? '未知', color: 'default' };
                return _jsx(Tag, { color: meta.color, children: row.statusLabel ?? meta.label });
            },
        },
        {
            title: '企业回传',
            width: 110,
            render: (_, row) => {
                const meta = returnReadyLabel(row);
                return _jsx(Tag, { color: meta.color, children: meta.text });
            },
        },
        {
            title: '机构方案材料',
            width: 220,
            render: (_, row) => (_jsx(Space, { size: "small", wrap: true, children: row.status >= 3 && (_jsxs(_Fragment, { children: [_jsx(Button, { type: "link", size: "small", icon: _jsx(CloudDownloadOutlined, {}), onClick: () => trainApi.downloadThreeYearPlanFile(row.id, 'reviewed'), children: "\u89C4\u5212\u8868" }), row.submitSchemeFileId ? (_jsx(Button, { type: "link", size: "small", onClick: () => trainApi.downloadThreeYearPlanFile(row.id, 'submit-scheme'), children: "\u65B9\u6848\u6587\u6863" })) : null] })) })),
        },
        { title: '提交时间', dataIndex: 'updateTime', width: 170, render: formatTime },
        {
            title: '驳回理由',
            dataIndex: 'rejectReason',
            width: 220,
            ellipsis: true,
            render: (v) => v || '-',
        },
        {
            title: '操作',
            width: 100,
            fixed: 'right',
            render: (_, row) => (_jsx(Button, { type: "link", icon: _jsx(EyeOutlined, {}), onClick: () => openDetail(row), children: "\u5BA1\u6838" })),
        },
    ];
    const renderDrawerActions = () => {
        if (!plan || plan.status !== 3)
            return null;
        const ready = isReturnReady(plan);
        const doConfirm = () => runAction(() => trainApi.companyConfirmThreeYearPlan(plan.id), '已确认方案');
        return (_jsxs(Space, { children: [_jsx(Button, { type: "primary", icon: _jsx(CheckOutlined, {}), loading: actionLoading, onClick: () => {
                        if (ready) {
                            void doConfirm();
                            return;
                        }
                        Modal.confirm({
                            title: '回传材料尚未齐套',
                            content: `${returnMissingHint(plan)}。确认后可在「已确认」详情中补交，是否继续确认方案？`,
                            okText: '仍要确认',
                            cancelText: '取消',
                            onOk: () => doConfirm(),
                        });
                    }, children: "\u786E\u8BA4\u65B9\u6848" }), _jsx(Button, { danger: true, icon: _jsx(CloseOutlined, {}), onClick: () => setRejectOpen(true), children: "\u9A73\u56DE" })] }));
    };
    const canUploadReturn = plan != null && (plan.status === 3 || plan.status === 4);
    const uploadReturnFile = async (kind, file) => {
        if (!plan)
            return false;
        setActionLoading(true);
        try {
            const updated = await trainApi.uploadThreeYearPlanEnterpriseReturn(plan.id, {
                rosterFile: kind === 'roster' ? file : undefined,
                shiftFile: kind === 'shift' ? file : undefined,
            });
            const ready = isReturnReady(updated);
            if (ready) {
                message.success(plan.status === 4 ? '回传材料已齐套' : '回传材料已齐套，可以确认方案');
            }
            else {
                message.success(`${kind === 'roster' ? '筛选名单' : '排班表'}已上传；${returnMissingHint(updated)}`
                    + (plan.status === 4 ? '，请继续补交' : '（确认后也可补交）'));
            }
            setCurrentPlan(updated);
            invalidateDetail(plan.id);
            refreshList();
        }
        catch (e) {
            message.error(e instanceof Error ? e.message : '上传失败');
        }
        finally {
            setActionLoading(false);
        }
        return false;
    };
    return (_jsxs(_Fragment, { children: [_jsx(Paragraph, { type: "secondary", style: { marginBottom: 16 }, children: "\u67E5\u770B\u57F9\u8BAD\u673A\u6784\u63D0\u4EA4\u7684\u4E09\u5E74\u89C4\u5212\u65B9\u6848\u53CA\u76F8\u5173\u6750\u6599\uFF0C\u786E\u8BA4\u6216\u9A73\u56DE\u540E\u8FDB\u5165\u4E0B\u4E00\u6D41\u7A0B\u3002" }), _jsx(Tabs, { activeKey: activeTab, onChange: (key) => {
                    setActiveTab(key);
                    setPage(1);
                }, items: [
                    { key: 'pending', label: '待审核' },
                    { key: 'confirmed', label: '已确认' },
                    { key: 'rejected', label: '已驳回' },
                    { key: 'all', label: '全部记录' },
                ] }), _jsxs(Space, { style: { marginBottom: 16 }, wrap: true, children: [_jsx(Input, { placeholder: "\u641C\u7D22\u65B9\u6848\u6807\u9898", allowClear: true, style: { width: 240 }, value: titleFilter, onChange: (e) => setTitleFilter(e.target.value), onPressEnter: () => { setPage(1); listQuery.refetch(); } }), _jsx(Button, { onClick: () => { setPage(1); listQuery.refetch(); }, children: "\u67E5\u8BE2" })] }), _jsx(Table, { rowKey: "id", columns: columns, dataSource: list, loading: listQuery.isLoading, scroll: { x: 1000 }, pagination: {
                    current: page,
                    pageSize,
                    total: activeTab === 'all' ? list.length : (listQuery.data?.total ?? 0),
                    onChange: (p, ps) => {
                        if (typeof ps === 'number' && ps > 0 && ps !== pageSize) {
                            setPageSize(ps);
                            setPage(1);
                        }
                        else {
                            setPage(p);
                        }
                    },
                    showTotal: (t) => `共 ${t} 条`,
                } }), _jsx(Drawer, { title: plan ? `方案审核：${plan.title}` : '方案审核', width: 960, open: detailOpen, onClose: closeDetail, extra: renderDrawerActions(), children: plan && (_jsxs(_Fragment, { children: [plan.status === 5 && plan.rejectReason && (_jsx(Alert, { type: "error", showIcon: true, style: { marginBottom: 16 }, message: "\u8BE5\u65B9\u6848\u5DF2\u88AB\u9A73\u56DE", description: _jsxs("div", { children: [_jsx("p", { style: { marginBottom: 4, color: '#666' }, children: "\u9A73\u56DE\u7406\u7531\uFF1A" }), _jsx("p", { style: { marginBottom: 0, whiteSpace: 'pre-wrap' }, children: plan.rejectReason })] }) })), _jsx(Alert, { type: "info", showIcon: true, icon: _jsx(FileTextOutlined, {}), message: "\u673A\u6784\u63D0\u4EA4\u65B9\u6848", description: "\u4EE5\u4E0B\u4E3A\u57F9\u8BAD\u673A\u6784\u63D0\u4EA4\u4F01\u4E1A\u5BA1\u6838\u65F6\u56FA\u5316\u7684\u65B9\u6848\u6750\u6599\uFF0C\u8BF7\u9884\u89C8\u6216\u4E0B\u8F7D\u67E5\u9605\u540E\u786E\u8BA4\u6216\u9A73\u56DE\u3002", style: { marginBottom: 16 } }), _jsx(Card, { size: "small", title: "\u65B9\u6848\u6750\u6599", style: { marginBottom: 16 }, children: _jsxs(Space, { direction: "vertical", size: "middle", style: { width: '100%' }, children: [plan.submitPersonnelFileId ? (_jsxs(Space, { wrap: true, children: [_jsxs(Typography.Text, { children: ["\u4F01\u4E1A\u5BA1\u6838\u4EBA\u5458\u540D\u5355", plan.submitPersonnelFileName ? `（${plan.submitPersonnelFileName}）` : ''] }), _jsx(Button, { type: "link", size: "small", icon: _jsx(EyeOutlined, {}), onClick: () => openMaterialPreview(plan.id, 'submit-personnel', plan.submitPersonnelFileName, '企业审核人员名单'), children: "\u9884\u89C8" }), _jsx(Button, { type: "link", size: "small", icon: _jsx(CloudDownloadOutlined, {}), onClick: () => trainApi.downloadThreeYearPlanFile(plan.id, 'submit-personnel'), children: "\u4E0B\u8F7D" })] })) : null, plan.submitSchemeFileId ? (_jsxs(Space, { wrap: true, children: [_jsxs(Typography.Text, { children: ["\u65B9\u6848\u6587\u6863", plan.submitSchemeFileName ? `（${plan.submitSchemeFileName}）` : ''] }), _jsx(Button, { type: "link", size: "small", icon: _jsx(EyeOutlined, {}), onClick: () => openMaterialPreview(plan.id, 'submit-scheme', plan.submitSchemeFileName, '方案文档'), children: "\u9884\u89C8" }), _jsx(Button, { type: "link", size: "small", icon: _jsx(CloudDownloadOutlined, {}), onClick: () => trainApi.downloadThreeYearPlanFile(plan.id, 'submit-scheme'), children: "\u4E0B\u8F7D" })] })) : null, !plan.submitPersonnelFileId && !plan.submitSchemeFileId ? (_jsx(Typography.Text, { type: "secondary", children: "\u6682\u65E0\u65B9\u6848\u6750\u6599" })) : null] }) }), _jsx(Alert, { type: plan.status === 4 && !isReturnReady(plan) ? 'warning' : 'info', showIcon: true, style: { marginBottom: 16 }, message: plan.status === 4 ? '回传材料（可补交）' : '企业回传', description: plan.status === 4 && !isReturnReady(plan)
                                ? `方案已确认，但${returnMissingHint(plan)}。请尽快补交，否则无法进入分班/排课。`
                                : plan.status === 4
                                    ? '方案已确认。如需更正，可重新上传覆盖当前回传版本。'
                                    : '请下载机构提交的人员名单，筛选后上传，并上传对应人员排班表。材料可不齐也可先确认，确认后可在「已确认」详情补交。' }), _jsx(Card, { size: "small", title: (_jsxs(Space, { children: [_jsx("span", { children: plan.status === 4 ? '回传材料（可补交）' : '企业回传材料' }), _jsx(Tag, { color: returnReadyLabel(plan).color, children: returnReadyLabel(plan).text })] })), style: { marginBottom: 16 }, children: _jsxs(Space, { direction: "vertical", size: "middle", style: { width: '100%' }, children: [_jsxs(Space, { wrap: true, align: "start", children: [_jsxs(Typography.Text, { children: ["\u7B5B\u9009\u540E\u57F9\u8BAD\u540D\u5355", plan.enterpriseRosterFileName ? `（${plan.enterpriseRosterFileName}）` : '（未上传）'] }), plan.enterpriseRosterFileId ? (_jsxs(_Fragment, { children: [_jsx(Button, { type: "link", size: "small", icon: _jsx(EyeOutlined, {}), onClick: () => openMaterialPreview(plan.id, 'enterprise-roster', plan.enterpriseRosterFileName, '企业筛选培训名单'), children: "\u9884\u89C8" }), _jsx(Button, { type: "link", size: "small", icon: _jsx(CloudDownloadOutlined, {}), onClick: () => trainApi.downloadThreeYearPlanFile(plan.id, 'enterprise-roster'), children: "\u4E0B\u8F7D" })] })) : null, canUploadReturn ? (_jsx(Upload, { accept: ".xlsx,.xls", showUploadList: false, beforeUpload: (file) => {
                                                    void uploadReturnFile('roster', file);
                                                    return false;
                                                }, children: _jsx(Button, { size: "small", icon: _jsx(CloudUploadOutlined, {}), loading: actionLoading, children: plan.enterpriseRosterFileId
                                                        ? (plan.status === 4 ? '覆盖上传' : '重新上传')
                                                        : (plan.status === 4 ? '补交名单' : '上传名单') }) })) : null] }), _jsxs(Space, { wrap: true, align: "start", children: [_jsxs(Typography.Text, { children: ["\u6392\u73ED\u8868", plan.enterpriseShiftFileName ? `（${plan.enterpriseShiftFileName}）` : '（未上传）'] }), plan.enterpriseShiftFileId ? (_jsxs(_Fragment, { children: [_jsx(Button, { type: "link", size: "small", icon: _jsx(EyeOutlined, {}), onClick: () => openMaterialPreview(plan.id, 'enterprise-shift', plan.enterpriseShiftFileName, '企业排班表'), children: "\u9884\u89C8" }), _jsx(Button, { type: "link", size: "small", icon: _jsx(CloudDownloadOutlined, {}), onClick: () => trainApi.downloadThreeYearPlanFile(plan.id, 'enterprise-shift'), children: "\u4E0B\u8F7D" })] })) : null, canUploadReturn ? (_jsx(Upload, { accept: ".xlsx,.xls", showUploadList: false, beforeUpload: (file) => {
                                                    void uploadReturnFile('shift', file);
                                                    return false;
                                                }, children: _jsx(Button, { size: "small", icon: _jsx(CloudUploadOutlined, {}), loading: actionLoading, children: plan.enterpriseShiftFileId
                                                        ? (plan.status === 4 ? '覆盖上传' : '重新上传')
                                                        : (plan.status === 4 ? '补交排班表' : '上传排班表') }) })) : null] }), plan.enterpriseReturnTime ? (_jsxs(Typography.Text, { type: "secondary", children: ["\u6700\u8FD1\u56DE\u4F20\uFF1A", plan.enterpriseReturnUsername || '企业', " \u00B7 ", formatTime(plan.enterpriseReturnTime)] })) : null] }) }), _jsxs(Descriptions, { column: 2, size: "small", bordered: true, style: { marginBottom: 16 }, children: [_jsx(Descriptions.Item, { label: "\u89C4\u5212\u7F16\u53F7", children: plan.planNo }), _jsx(Descriptions.Item, { label: "\u72B6\u6001", children: _jsx(Tag, { color: STATUS_MAP[plan.status]?.color, children: plan.statusLabel ?? STATUS_MAP[plan.status]?.label }) }), _jsx(Descriptions.Item, { label: "\u76EE\u6807\u4F01\u4E1A", children: plan.targetOrgName }), _jsx(Descriptions.Item, { label: "\u89C4\u5212\u5E74\u4EFD", children: plan.planYears }), _jsx(Descriptions.Item, { label: "\u63A8\u8350\u8BA4\u5B9A\u4EBA\u6570", children: plan.recommendedRows ?? '-' }), _jsx(Descriptions.Item, { label: "\u63D0\u4EA4\u65F6\u95F4", children: formatTime(plan.updateTime) }), plan.rejectReason && (_jsx(Descriptions.Item, { label: "\u9A73\u56DE\u7406\u7531", span: 2, children: plan.rejectReason })), plan.companyConfirmUsername && (_jsxs(Descriptions.Item, { label: "\u786E\u8BA4\u4FE1\u606F", span: 2, children: [plan.companyConfirmUsername, " \u00B7 ", formatTime(plan.companyConfirmTime)] }))] }), plan.rejectHistory && plan.rejectHistory.length > 0 && (_jsx(Card, { size: "small", title: "\u9A73\u56DE\u5386\u53F2", style: { marginTop: 16 }, children: renderRejectHistory(plan.rejectHistory) }))] })) }), _jsx(FilePreviewModal, { open: previewTarget != null, target: previewTarget, onClose: () => setPreviewTarget(null) }), _jsx(Modal, { title: "\u9A73\u56DE\u65B9\u6848", open: rejectOpen, onCancel: () => { setRejectOpen(false); setRejectReason(''); }, onOk: async () => {
                    if (!plan)
                        return;
                    if (!rejectReason.trim()) {
                        message.warning('请填写驳回理由');
                        return;
                    }
                    await runAction(() => trainApi.companyRejectThreeYearPlan(plan.id, rejectReason.trim()), '已驳回');
                    setRejectOpen(false);
                    setRejectReason('');
                }, confirmLoading: actionLoading, destroyOnClose: true, children: _jsx(Input.TextArea, { rows: 4, placeholder: "\u8BF7\u586B\u5199\u9A73\u56DE\u7406\u7531\uFF0C\u673A\u6784\u5C06\u636E\u6B64\u8C03\u6574\u65B9\u6848", value: rejectReason, onChange: (e) => setRejectReason(e.target.value) }) })] }));
}

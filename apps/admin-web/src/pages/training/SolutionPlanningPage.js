import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useCallback, useEffect, useRef, useState } from 'react';
import { Alert, Button, Card, Descriptions, Divider, Drawer, Dropdown, Flex, Form, Input, InputNumber, Modal, Progress, Select, Space, Table, Tabs, Tag, Timeline, Typography, Upload, message, } from 'antd';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { CheckOutlined, CloseOutlined, CloudDownloadOutlined, CloudUploadOutlined, DeleteOutlined, DownOutlined, EditOutlined, EyeOutlined, PlusOutlined, ReloadOutlined, SendOutlined, } from '@ant-design/icons';
import * as trainApi from '../../api/training';
import { useAuthStore } from '../../stores/authStore';
import { usePaneActive } from '../../layouts/TabKeepAlive';
import { PersonnelCertImportPanel } from './PersonnelCertImportPanel';
import { TrainingProcessSteps } from './TrainingProcessSteps';
const { Text } = Typography;
const STATUS_MAP = {
    0: { label: '草稿', color: 'default' },
    1: { label: '分析中', color: 'processing' },
    2: { label: '分析完成请审核', color: 'error' },
    3: { label: '待机构审核', color: 'warning' },
    4: { label: '企业已确认', color: 'success' },
    5: { label: '已驳回', color: 'error' },
    6: { label: '已作废', color: 'default' },
};
function formatTime(value) {
    if (!value)
        return '-';
    return value.replace('T', ' ').slice(0, 19);
}
function isRejectedPlan(plan) {
    return !!plan && planStatusCode(plan.status) === 5;
}
function renderRejectHistory(items) {
    if (!items || items.length === 0) {
        return _jsx(Text, { type: "secondary", children: "\u6682\u65E0\u9A73\u56DE\u5386\u53F2" });
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
function renderSubmitHistory(items) {
    if (!items || items.length === 0) {
        return _jsx(Text, { type: "secondary", children: "\u6682\u65E0\u63D0\u4EA4\u5386\u53F2" });
    }
    return (_jsx(Timeline, { items: items
            .slice()
            .reverse()
            .map((item, idx) => ({
            key: idx,
            color: 'blue',
            children: (_jsxs("div", { children: [_jsxs("div", { style: { fontWeight: 600 }, children: [item.submittedByUsername || '机构', item.submittedAt ? ` · ${formatTime(item.submittedAt)}` : ''] }), (item.personnelFileName || item.schemeFileName) && (_jsxs("div", { style: { fontSize: 12, color: '#999' }, children: [item.personnelFileName ? `人员名单：${item.personnelFileName}` : '', item.personnelFileName && item.schemeFileName ? '；' : '', item.schemeFileName ? `方案文档：${item.schemeFileName}` : ''] }))] })),
        })) }));
}
function planStatusCode(status) {
    return Number(status ?? 0);
}
function isPlanAnalyzing(row) {
    return planStatusCode(row.status) === 1;
}
function isPlanSyncing(row) {
    if (!isPlanAnalyzing(row))
        return false;
    if (row.analysisStatus === 'syncing')
        return true;
    const rencai = row.rencaiTaskStatus ?? '';
    return rencai === 'COMPLETED' || rencai === 'PENDING_MANUAL';
}
function isPlatformAdminUser(roles, username) {
    const roleSet = new Set((roles ?? []).map((r) => String(r).toUpperCase()));
    return roleSet.has('PLATFORM_ADMIN') || roleSet.has('SUPER_ADMIN') || username === 'admin';
}
export const SolutionPlanningPage = () => {
    const queryClient = useQueryClient();
    const paneActive = usePaneActive(); // 标签页不可见时暂停轮询（§3.6）
    const loginRole = useAuthStore((s) => s.currentUser?.loginRole);
    const roles = useAuthStore((s) => s.currentUser?.roles ?? []);
    const username = useAuthStore((s) => s.currentUser?.username);
    const authToken = useAuthStore((s) => s.token);
    const authTenantId = useAuthStore((s) => s.tenantId);
    const reviewIframeRef = useRef(null);
    const reviewTargetOrigin = `${window.location.protocol}//${window.location.host}`;
    /** 与知识库嵌入页一致：向 rencaishibie iframe 下发 SaaS Bearer token */
    const sendReviewAuth = useCallback(() => {
        if (!reviewIframeRef.current?.contentWindow || !authToken)
            return;
        reviewIframeRef.current.contentWindow.postMessage({ type: 'saas-auth', token: authToken, tenantId: authTenantId }, reviewTargetOrigin);
    }, [authToken, authTenantId, reviewTargetOrigin]);
    const isPlatformAdmin = isPlatformAdminUser(roles, username);
    /** 企业账号仅查看/确认；平台超级管理员始终视为机构操作员 */
    const isEnterpriseViewer = loginRole === 'company' && !isPlatformAdmin;
    const canManagePlan = !isEnterpriseViewer;
    const [activeTab, setActiveTab] = useState('three-year');
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(10);
    const [statusFilter, setStatusFilter] = useState();
    const [titleFilter, setTitleFilter] = useState('');
    const [createOpen, setCreateOpen] = useState(false);
    const [createForm] = Form.useForm();
    const [fileList, setFileList] = useState([]);
    const [creating, setCreating] = useState(false);
    const [detailOpen, setDetailOpen] = useState(false);
    const [currentPlan, setCurrentPlan] = useState(null);
    const [itemPage, setItemPage] = useState(1);
    const [itemPageSize, setItemPageSize] = useState(20);
    const [itemYearFilter, setItemYearFilter] = useState();
    const [actionLoading, setActionLoading] = useState(false);
    const [rejectOpen, setRejectOpen] = useState(false);
    const [rejectReason, setRejectReason] = useState('');
    const [submitOpen, setSubmitOpen] = useState(false);
    const [submitPersonnelList, setSubmitPersonnelList] = useState([]);
    const [submitSchemeList, setSubmitSchemeList] = useState([]);
    const [submitting, setSubmitting] = useState(false);
    const [reviewOpen, setReviewOpen] = useState(false);
    const [editItemOpen, setEditItemOpen] = useState(false);
    const [editingItem, setEditingItem] = useState(null);
    const [itemForm] = Form.useForm();
    const [savingItem, setSavingItem] = useState(false);
    const detailRetryCountRef = useRef(0);
    const listQuery = useQuery({
        queryKey: ['three-year-plans', page, pageSize, statusFilter, titleFilter],
        queryFn: () => trainApi.pageThreeYearPlans({
            page,
            pageSize,
            status: statusFilter,
            title: titleFilter || undefined,
        }),
        enabled: activeTab === 'three-year',
        refetchInterval: (query) => {
            if (!paneActive)
                return false;
            const items = query.state.data?.items ?? [];
            return items.some((p) => planStatusCode(p.status) === 1) ? 3000 : false;
        },
    });
    const publicDataQuery = useQuery({
        queryKey: ['demand-public-data', 'company'],
        queryFn: () => trainApi.getDemandPublicData(['company']),
    });
    const researchQuery = useQuery({
        queryKey: ['research-orgs-all'],
        queryFn: () => trainApi.pageResearchOrgs({ page: 1, pageSize: 200, status: 1 }),
    });
    const detailQuery = useQuery({
        queryKey: ['three-year-plan', currentPlan?.id],
        queryFn: () => trainApi.getThreeYearPlan(currentPlan.id),
        enabled: detailOpen && !!currentPlan?.id && Number(currentPlan?.status ?? 0) < 2,
        refetchInterval: (query) => {
            if (!paneActive)
                return false;
            const plan = query.state.data;
            return plan?.status === 1 ? 3000 : false;
        },
    });
    /** 轮询或列表行状态 >=2 时即触发 bundle，不等待 currentPlan 更新 */
    const statusForBundleFetch = Number(detailQuery.data?.status ?? currentPlan?.status ?? 0);
    const detailBundleQuery = useQuery({
        queryKey: ['three-year-plan-bundle', currentPlan?.id, itemPage, itemPageSize, itemYearFilter],
        queryFn: () => trainApi.getThreeYearPlanDetailBundle(currentPlan.id, {
            page: itemPage,
            pageSize: itemPageSize,
            planYear: itemYearFilter,
        }),
        enabled: detailOpen && !!currentPlan?.id && statusForBundleFetch >= 2,
        staleTime: 30_000,
    });
    const resolvedPlanStatus = Number(detailBundleQuery.data?.plan?.status
        ?? detailQuery.data?.status
        ?? currentPlan?.status
        ?? 0);
    const plan = detailBundleQuery.data?.plan ?? detailQuery.data ?? currentPlan;
    const bundleItems = detailBundleQuery.data?.items;
    const bundleMatrix = detailBundleQuery.data?.matrix;
    const bundleDistribution = detailBundleQuery.data?.distribution;
    const companyList = publicDataQuery.data?.company_list ?? [];
    const researchList = researchQuery.data?.items ?? [];
    const refreshList = useCallback(() => {
        queryClient.invalidateQueries({ queryKey: ['three-year-plans'] });
    }, [queryClient]);
    const openDetail = (row) => {
        detailRetryCountRef.current = 0;
        setCurrentPlan(row);
        setItemPage(1);
        setItemYearFilter(undefined);
        setDetailOpen(true);
    };
    const closeDetail = useCallback((planId) => {
        setDetailOpen(false);
        setCurrentPlan(null);
        setItemPage(1);
        setItemYearFilter(undefined);
        if (planId != null) {
            queryClient.removeQueries({ queryKey: ['three-year-plan', planId] });
            queryClient.removeQueries({ queryKey: ['three-year-plan-bundle', planId] });
        }
    }, [queryClient]);
    const invalidateDetailData = useCallback((planId) => {
        queryClient.invalidateQueries({ queryKey: ['three-year-plan', planId] });
        queryClient.invalidateQueries({ queryKey: ['three-year-plan-bundle', planId] });
    }, [queryClient]);
    const openEditItem = (item) => {
        if (!item.id) {
            message.warning('明细尚未落库，请稍后刷新再编辑');
            return;
        }
        setEditingItem(item);
        itemForm.setFieldsValue({
            personName: item.personName,
            jobName: item.jobName,
            levelName: item.levelName,
            eligibleDate: item.eligibleDate,
            planYear: item.planYear,
        });
        setEditItemOpen(true);
    };
    const handleSaveItem = async () => {
        if (!plan?.id || !editingItem?.id)
            return;
        try {
            const values = await itemForm.validateFields();
            setSavingItem(true);
            await trainApi.updateThreeYearPlanItem(plan.id, editingItem.id, values);
            message.success('已保存');
            setEditItemOpen(false);
            setEditingItem(null);
            itemForm.resetFields();
            invalidateDetailData(plan.id);
        }
        catch (e) {
            if (e instanceof Error && e.message) {
                message.error(e.message);
            }
        }
        finally {
            setSavingItem(false);
        }
    };
    const handleCreate = async () => {
        try {
            const values = await createForm.validateFields();
            const file = fileList[0]?.originFileObj;
            if (!file) {
                message.warning('请上传员工 Excel 文件');
                return;
            }
            setCreating(true);
            await trainApi.createThreeYearPlan({
                title: values.title,
                targetOrgId: values.targetOrgId,
                researchOrgId: values.researchOrgId,
                file,
            });
            message.success('已创建并开始人才识别分析');
            setCreateOpen(false);
            createForm.resetFields();
            setFileList([]);
            refreshList();
        }
        catch (e) {
            if (e instanceof Error && e.message) {
                message.error(e.message);
            }
        }
        finally {
            setCreating(false);
        }
    };
    const runAction = async (fn, successMsg, options) => {
        setActionLoading(true);
        try {
            await fn();
            message.success(successMsg);
            refreshList();
            if (options?.refreshDetail !== false && detailOpen && currentPlan?.id) {
                invalidateDetailData(currentPlan.id);
            }
        }
        catch (e) {
            message.error(e instanceof Error ? e.message : '操作失败');
        }
        finally {
            setActionLoading(false);
        }
    };
    const handleDeletePlan = async (planId) => {
        setActionLoading(true);
        try {
            await trainApi.deleteThreeYearPlan(planId);
            closeDetail(planId);
            message.success('已删除');
            refreshList();
        }
        catch (e) {
            message.error(e instanceof Error ? e.message : '操作失败');
        }
        finally {
            setActionLoading(false);
        }
    };
    const handleSubmitEnterprise = async () => {
        if (!plan)
            return;
        const personnelFile = submitPersonnelList[0]?.originFileObj;
        const schemeFile = submitSchemeList[0]?.originFileObj;
        if (!personnelFile) {
            message.warning('请上传企业审核人员名单');
            return;
        }
        if (!schemeFile) {
            message.warning('请上传方案文档');
            return;
        }
        setSubmitting(true);
        try {
            await trainApi.submitThreeYearPlan(plan.id, { personnelFile, schemeFile });
            message.success('已提交，已生成已审核规划表并进入待机构审核');
            setSubmitOpen(false);
            setSubmitPersonnelList([]);
            setSubmitSchemeList([]);
            refreshList();
            invalidateDetailData(plan.id);
        }
        catch (e) {
            message.error(e instanceof Error ? e.message : '提交失败');
        }
        finally {
            setSubmitting(false);
        }
    };
    useEffect(() => {
        const latestPlan = detailBundleQuery.data?.plan ?? detailQuery.data;
        if (latestPlan) {
            setCurrentPlan(latestPlan);
        }
    }, [detailBundleQuery.data, detailQuery.data]);
    /** 分析完成但明细尚未同步时自动重试（兼容 rencaishibie 入库竞态） */
    useEffect(() => {
        if (!detailOpen || !currentPlan?.id) {
            detailRetryCountRef.current = 0;
            return;
        }
        const recommended = Number(plan?.recommendedRows ?? 0);
        const itemTotal = bundleItems?.total ?? 0;
        if (resolvedPlanStatus >= 2
            && recommended > 0
            && itemTotal === 0
            && !detailBundleQuery.isFetching
            && detailRetryCountRef.current < 10) {
            const timer = window.setTimeout(() => {
                detailRetryCountRef.current += 1;
                detailBundleQuery.refetch();
            }, 2000);
            return () => window.clearTimeout(timer);
        }
        if (itemTotal > 0) {
            detailRetryCountRef.current = 0;
        }
    }, [
        detailOpen,
        currentPlan?.id,
        resolvedPlanStatus,
        plan?.recommendedRows,
        bundleItems?.total,
        detailBundleQuery.isFetching,
        detailBundleQuery,
    ]);
    const columns = [
        { title: '规划编号', dataIndex: 'planNo', width: 180 },
        { title: '标题', dataIndex: 'title', ellipsis: true },
        { title: '目标企业', dataIndex: 'targetOrgName', width: 160, ellipsis: true },
        {
            title: '状态',
            dataIndex: 'status',
            width: 150,
            render: (status, row) => {
                const code = planStatusCode(status);
                const meta = STATUS_MAP[code] ?? { label: row.statusLabel ?? '未知', color: 'default' };
                const label = row.statusLabel ?? meta.label;
                const color = isPlanSyncing(row) ? 'processing' : meta.color;
                return _jsx(Tag, { color: color, children: label });
            },
        },
        {
            title: '企业回传',
            width: 140,
            render: (_, row) => {
                const code = planStatusCode(row.status);
                if (code < 3)
                    return '-';
                const ready = !!(row.enterpriseRosterFileId && row.enterpriseShiftFileId);
                if (code === 4) {
                    return ready
                        ? _jsx(Tag, { color: "success", children: "\u5DF2\u786E\u8BA4 \u00B7 \u6750\u6599\u5DF2\u9F50" })
                        : _jsx(Tag, { color: "warning", children: "\u5DF2\u786E\u8BA4 \u00B7 \u5F85\u8865\u4EA4" });
                }
                if (ready)
                    return _jsx(Tag, { color: "processing", children: "\u6750\u6599\u5DF2\u9F50" });
                if (row.enterpriseRosterFileId || row.enterpriseShiftFileId) {
                    return _jsx(Tag, { color: "warning", children: "\u90E8\u5206\u56DE\u4F20" });
                }
                return _jsx(Tag, { children: "\u672A\u56DE\u4F20" });
            },
        },
        {
            title: '分析进度',
            width: 140,
            render: (_, row) => {
                if (!isPlanAnalyzing(row))
                    return '-';
                if (isPlanSyncing(row)) {
                    return _jsx(Progress, { percent: 100, size: "small", status: "active", format: () => '同步中' });
                }
                const pct = Math.round(row.progressPct ?? 0);
                return _jsx(Progress, { percent: pct, size: "small", status: pct >= 99 ? 'active' : undefined });
            },
        },
        { title: '创建时间', dataIndex: 'createTime', width: 170, render: formatTime },
        {
            title: '操作',
            width: 100,
            fixed: 'right',
            render: (_, row) => (_jsx(Button, { type: "link", icon: _jsx(EyeOutlined, {}), onClick: () => openDetail(row), children: "\u8BE6\u60C5" })),
        },
    ];
    const itemColumns = [
        { title: '姓名', dataIndex: 'personName', width: 90 },
        { title: '单位', dataIndex: 'companyName', ellipsis: true },
        { title: '工种', dataIndex: 'jobName', width: 120 },
        { title: '等级', dataIndex: 'levelName', width: 80 },
        { title: '认定时间', dataIndex: 'eligibleDate', width: 90 },
        { title: '规划年', dataIndex: 'planYear', width: 70 },
        ...(canManagePlan && (plan?.status === 2 || plan?.status === 5)
            ? [{
                    title: '操作',
                    width: 70,
                    fixed: 'right',
                    render: (_, row) => (_jsx(Button, { type: "link", size: "small", icon: _jsx(EditOutlined, {}), disabled: !row.id, onClick: () => openEditItem(row), children: "\u7F16\u8F91" })),
                }]
            : []),
    ];
    const distributionColumns = [
        { title: '工种', dataIndex: 'jobName', width: 140 },
        { title: '等级', dataIndex: 'levelName', width: 90 },
        { title: '人数', dataIndex: 'employeeCount', width: 70 },
        { title: '开班阈值', dataIndex: 'thresholdMin', width: 90 },
        {
            title: '达阈值',
            dataIndex: 'meetsThreshold',
            width: 80,
            render: (v) => (v ? _jsx(Tag, { color: "success", children: "\u662F" }) : _jsx(Tag, { color: "warning", children: "\u5426" })),
        },
        { title: '提示', dataIndex: 'warning', ellipsis: true },
    ];
    const matrixColumns = [
        { title: '工种', dataIndex: 'jobName' },
        { title: '规划年', dataIndex: 'planYear', width: 90 },
        { title: '等级', dataIndex: 'levelName', width: 100 },
        { title: '人数', dataIndex: 'headcount', width: 80 },
    ];
    const renderDetailToolbar = () => {
        if (!plan)
            return null;
        const workflowActions = [];
        const downloadMenuItems = [];
        if (planStatusCode(plan.status) === 1) {
            workflowActions.push(_jsx(Button, { icon: _jsx(ReloadOutlined, {}), loading: actionLoading, onClick: () => runAction(() => trainApi.refreshThreeYearPlanAnalysis(plan.id), '已刷新分析状态'), children: "\u5237\u65B0\u8FDB\u5EA6" }, "refresh"));
        }
        if (plan.status >= 2 && plan.rencaiTaskId) {
            workflowActions.push(_jsx(Button, { onClick: () => setReviewOpen(true), children: "\u5BA1\u6838\u5DE5\u4F5C\u53F0" }, "review"));
        }
        if (canManagePlan && (plan.status === 2 || plan.status === 5)) {
            workflowActions.push(_jsx(Button, { type: "primary", icon: _jsx(SendOutlined, {}), onClick: () => setSubmitOpen(true), children: plan.status === 5 ? '重新提交企业审核' : '提交企业审核' }, "submit"));
        }
        if (isEnterpriseViewer && plan.status === 3) {
            const returnReady = !!(plan.enterpriseRosterFileId && plan.enterpriseShiftFileId);
            const missingReturn = [];
            if (!plan.enterpriseRosterFileId)
                missingReturn.push('筛选后培训名单');
            if (!plan.enterpriseShiftFileId)
                missingReturn.push('排班表');
            const doConfirm = () => runAction(() => trainApi.companyConfirmThreeYearPlan(plan.id), '已确认');
            workflowActions.push(_jsx(Button, { type: "primary", icon: _jsx(CheckOutlined, {}), loading: actionLoading, onClick: () => {
                    if (returnReady) {
                        void doConfirm();
                        return;
                    }
                    Modal.confirm({
                        title: '回传材料尚未齐套',
                        content: `还缺：${missingReturn.join('、') || '筛选名单与排班表'}。确认后可在已确认详情中补交，是否继续？`,
                        okText: '仍要确认',
                        cancelText: '取消',
                        onOk: () => doConfirm(),
                    });
                }, children: "\u4F01\u4E1A\u786E\u8BA4" }, "confirm"), _jsx(Button, { danger: true, icon: _jsx(CloseOutlined, {}), onClick: () => setRejectOpen(true), children: "\u4F01\u4E1A\u9A73\u56DE" }, "reject"));
        }
        if (plan.status >= 2) {
            downloadMenuItems.push({
                key: 'reviewed',
                icon: _jsx(CloudDownloadOutlined, {}),
                label: '已审核规划表',
                onClick: () => trainApi.downloadThreeYearPlanFile(plan.id, 'reviewed'),
            });
        }
        if (plan.status >= 2 && plan.rencaiTaskId) {
            downloadMenuItems.push({
                key: 'plan',
                icon: _jsx(CloudDownloadOutlined, {}),
                label: '原始规划表',
                onClick: () => trainApi.downloadThreeYearPlanFile(plan.id, 'plan'),
            }, {
                key: 'result',
                icon: _jsx(CloudDownloadOutlined, {}),
                label: '原始认定结果',
                onClick: () => trainApi.downloadThreeYearPlanFile(plan.id, 'result'),
            });
        }
        if (plan.status >= 3 && plan.submitPersonnelFileId) {
            downloadMenuItems.push({
                key: 'submit-personnel',
                icon: _jsx(CloudDownloadOutlined, {}),
                label: '审核人员名单',
                onClick: () => trainApi.downloadThreeYearPlanFile(plan.id, 'submit-personnel'),
            });
        }
        if (plan.status >= 3 && plan.submitSchemeFileId) {
            downloadMenuItems.push({
                key: 'submit-scheme',
                icon: _jsx(CloudDownloadOutlined, {}),
                label: '方案文档',
                onClick: () => trainApi.downloadThreeYearPlanFile(plan.id, 'submit-scheme'),
            });
        }
        if (plan.status >= 3 && plan.enterpriseRosterFileId) {
            downloadMenuItems.push({
                key: 'enterprise-roster',
                icon: _jsx(CloudDownloadOutlined, {}),
                label: '企业筛选培训名单',
                onClick: () => trainApi.downloadThreeYearPlanFile(plan.id, 'enterprise-roster'),
            });
        }
        if (plan.status >= 3 && plan.enterpriseShiftFileId) {
            downloadMenuItems.push({
                key: 'enterprise-shift',
                icon: _jsx(CloudDownloadOutlined, {}),
                label: '企业排班表',
                onClick: () => trainApi.downloadThreeYearPlanFile(plan.id, 'enterprise-shift'),
            });
        }
        const hasDownloads = (downloadMenuItems?.length ?? 0) > 0;
        const showDelete = canManagePlan && plan.status !== 3;
        if (!workflowActions.length && !hasDownloads && !showDelete) {
            return null;
        }
        return (_jsxs(Flex, { wrap: "wrap", gap: 8, align: "center", justify: "flex-end", children: [workflowActions.length > 0 && (_jsx(Space, { wrap: true, size: 8, children: workflowActions })), workflowActions.length > 0 && (hasDownloads || showDelete) && (_jsx(Divider, { type: "vertical", style: { margin: 0, height: 20 } })), hasDownloads && (_jsx(Dropdown, { menu: { items: downloadMenuItems }, children: _jsxs(Button, { icon: _jsx(CloudDownloadOutlined, {}), children: ["\u4E0B\u8F7D\u6587\u4EF6 ", _jsx(DownOutlined, {})] }) })), showDelete && (_jsx(Button, { danger: true, icon: _jsx(DeleteOutlined, {}), loading: actionLoading, onClick: () => {
                        Modal.confirm({
                            title: '确认删除该三年规划？',
                            onOk: () => handleDeletePlan(plan.id),
                        });
                    }, children: "\u5220\u9664" }))] }));
    };
    return (_jsxs(_Fragment, { children: [_jsx(TrainingProcessSteps, { current: 1 }), _jsxs(Card, { bordered: false, bodyStyle: { padding: 0 }, children: [_jsx(Tabs, { activeKey: activeTab, onChange: setActiveTab, type: "card", style: { padding: '16px 16px 0' }, items: [
                            {
                                key: 'three-year',
                                label: '三年规划',
                                children: (_jsxs("div", { style: { padding: 16 }, children: [_jsxs(Space, { style: { marginBottom: 16 }, wrap: true, children: [_jsx(Input, { placeholder: "\u641C\u7D22\u6807\u9898", allowClear: true, style: { width: 200 }, value: titleFilter, onChange: (e) => setTitleFilter(e.target.value), onPressEnter: () => { setPage(1); refreshList(); } }), _jsx(Select, { allowClear: true, placeholder: "\u72B6\u6001\u7B5B\u9009", style: { width: 160 }, value: statusFilter, onChange: setStatusFilter, options: Object.entries(STATUS_MAP).map(([k, v]) => ({
                                                        value: Number(k),
                                                        label: v.label,
                                                    })) }), _jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: refreshList, children: "\u5237\u65B0" }), canManagePlan && (_jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), onClick: () => setCreateOpen(true), children: "\u65B0\u5EFA\u4E09\u5E74\u89C4\u5212" }))] }), _jsx(Table, { rowKey: "id", loading: listQuery.isLoading, columns: columns, dataSource: listQuery.data?.items ?? [], scroll: { x: 1100 }, pagination: {
                                                current: page,
                                                pageSize,
                                                total: listQuery.data?.total ?? 0,
                                                showSizeChanger: true,
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
                            {
                                key: 'training-plan',
                                label: '人员认定',
                                children: _jsx(PersonnelCertImportPanel, {}),
                            },
                        ] }), _jsx(Modal, { title: "\u65B0\u5EFA\u4E09\u5E74\u89C4\u5212", open: createOpen, onCancel: () => setCreateOpen(false), onOk: handleCreate, confirmLoading: creating, destroyOnClose: true, width: 560, children: _jsxs(Form, { form: createForm, layout: "vertical", children: [_jsx(Form.Item, { name: "title", label: "\u89C4\u5212\u6807\u9898", rules: [{ required: true, message: '请输入标题' }], children: _jsx(Input, { placeholder: "\u5982\uFF1A\u67D0\u67D0\u4F01\u4E1A 2026-2028 \u6280\u80FD\u8BA4\u5B9A\u4E09\u5E74\u89C4\u5212" }) }), _jsx(Form.Item, { name: "targetOrgId", label: "\u76EE\u6807\u4F01\u4E1A", rules: [{ required: true, message: '请选择企业' }], children: _jsx(Select, { showSearch: true, placeholder: "\u9009\u62E9\u4F01\u4E1A", optionFilterProp: "label", options: companyList.map((c) => ({ value: c.id, label: c.title })) }) }), _jsx(Form.Item, { name: "researchOrgId", label: "\u5173\u8054\u4F01\u4E1A\u8C03\u7814\uFF08\u53EF\u9009\uFF09", children: _jsx(Select, { allowClear: true, showSearch: true, placeholder: "\u9009\u62E9\u8C03\u7814\u8BB0\u5F55", optionFilterProp: "label", options: researchList.map((r) => ({ value: r.id, label: r.title })) }) }), _jsx(Form.Item, { label: "\u5458\u5DE5 Excel", required: true, children: _jsx(Upload, { accept: ".xlsx,.xls", maxCount: 1, fileList: fileList, beforeUpload: () => false, onChange: ({ fileList: fl }) => setFileList(fl), children: _jsx(Button, { children: "\u9009\u62E9 Excel \u6587\u4EF6" }) }) })] }) }), _jsx(Drawer, { title: plan ? (_jsxs(Text, { ellipsis: { tooltip: plan.title }, style: { maxWidth: '100%' }, children: ["\u4E09\u5E74\u89C4\u5212\uFF1A", plan.title] })) : '三年规划详情', width: 960, open: detailOpen, onClose: () => closeDetail(currentPlan?.id), children: plan && (_jsxs(_Fragment, { children: [(() => {
                                    const toolbar = renderDetailToolbar();
                                    if (!toolbar)
                                        return null;
                                    return (_jsx("div", { style: {
                                            margin: '-24px -24px 16px',
                                            padding: '12px 24px',
                                            background: '#fafafa',
                                            borderBottom: '1px solid #f0f0f0',
                                        }, children: toolbar }));
                                })(), isRejectedPlan(plan) && plan.rejectReason && (_jsx(Alert, { type: "error", showIcon: true, style: { marginBottom: 16 }, message: "\u8BE5\u65B9\u6848\u5DF2\u88AB\u4F01\u4E1A\u9A73\u56DE", description: _jsxs("div", { children: [_jsx("p", { style: { marginBottom: 4, color: '#666' }, children: "\u9A73\u56DE\u7406\u7531\uFF1A" }), _jsx("p", { style: { marginBottom: 8, whiteSpace: 'pre-wrap' }, children: plan.rejectReason }), _jsx("p", { style: { marginBottom: 0, color: '#999', fontSize: 12 }, children: "\u8BF7\u6839\u636E\u9A73\u56DE\u7406\u7531\u4FEE\u6539\u65B9\u6848\u4FE1\u606F\u540E\uFF0C\u70B9\u51FB\u300C\u91CD\u65B0\u63D0\u4EA4\u4F01\u4E1A\u5BA1\u6838\u300D" })] }) })), plan.status === 4 && !(plan.enterpriseRosterFileId && plan.enterpriseShiftFileId) && (_jsx(Alert, { type: "warning", showIcon: true, style: { marginBottom: 16 }, message: "\u5DF2\u786E\u8BA4 \u00B7 \u5F85\u8865\u4EA4\u56DE\u4F20\u6750\u6599", description: [
                                        !plan.enterpriseRosterFileId ? '筛选后培训名单' : null,
                                        !plan.enterpriseShiftFileId ? '排班表' : null,
                                    ].filter(Boolean).join('、')
                                        + '尚未上传。请尽快补交，补齐前不建议进入分班/排课。' })), (plan.status === 3 || plan.status === 4) && (canManagePlan || isEnterpriseViewer) && (_jsx(Alert, { type: "info", showIcon: true, style: { marginBottom: 16 }, message: plan.status === 4 ? '回传材料补交 / 覆盖' : '企业回传材料上传', description: (_jsxs(Space, { wrap: true, children: [_jsx(Upload, { accept: ".xlsx,.xls", showUploadList: false, beforeUpload: (file) => {
                                                    void (async () => {
                                                        setActionLoading(true);
                                                        try {
                                                            const updated = await trainApi.uploadThreeYearPlanEnterpriseReturn(plan.id, {
                                                                rosterFile: file,
                                                            });
                                                            message.success('筛选名单已上传');
                                                            setCurrentPlan(updated);
                                                            invalidateDetailData(plan.id);
                                                            refreshList();
                                                        }
                                                        catch (e) {
                                                            message.error(e instanceof Error ? e.message : '上传失败');
                                                        }
                                                        finally {
                                                            setActionLoading(false);
                                                        }
                                                    })();
                                                    return false;
                                                }, children: _jsx(Button, { size: "small", icon: _jsx(CloudUploadOutlined, {}), loading: actionLoading, children: plan.enterpriseRosterFileId
                                                        ? (plan.status === 4 ? '覆盖名单' : '重新上传名单')
                                                        : (plan.status === 4 ? '补交名单' : '上传名单') }) }), _jsx(Upload, { accept: ".xlsx,.xls", showUploadList: false, beforeUpload: (file) => {
                                                    void (async () => {
                                                        setActionLoading(true);
                                                        try {
                                                            const updated = await trainApi.uploadThreeYearPlanEnterpriseReturn(plan.id, {
                                                                shiftFile: file,
                                                            });
                                                            message.success('排班表已上传');
                                                            setCurrentPlan(updated);
                                                            invalidateDetailData(plan.id);
                                                            refreshList();
                                                        }
                                                        catch (e) {
                                                            message.error(e instanceof Error ? e.message : '上传失败');
                                                        }
                                                        finally {
                                                            setActionLoading(false);
                                                        }
                                                    })();
                                                    return false;
                                                }, children: _jsx(Button, { size: "small", icon: _jsx(CloudUploadOutlined, {}), loading: actionLoading, children: plan.enterpriseShiftFileId
                                                        ? (plan.status === 4 ? '覆盖排班表' : '重新上传排班表')
                                                        : (plan.status === 4 ? '补交排班表' : '上传排班表') }) })] })) })), _jsxs(Descriptions, { column: 2, size: "small", bordered: true, style: { marginBottom: 16 }, children: [_jsx(Descriptions.Item, { label: "\u89C4\u5212\u7F16\u53F7", children: plan.planNo }), _jsx(Descriptions.Item, { label: "\u72B6\u6001", children: _jsx(Tag, { color: STATUS_MAP[plan.status]?.color, children: plan.statusLabel ?? STATUS_MAP[plan.status]?.label }) }), _jsx(Descriptions.Item, { label: "\u76EE\u6807\u4F01\u4E1A", children: plan.targetOrgName }), _jsx(Descriptions.Item, { label: "\u89C4\u5212\u5E74\u4EFD", children: plan.planYears }), _jsx(Descriptions.Item, { label: "\u4EBA\u624D\u8BC6\u522B\u4EFB\u52A1", children: plan.rencaiTaskId ?? '-' }), _jsx(Descriptions.Item, { label: "\u63A8\u8350\u4EBA\u6570", children: plan.recommendedRows ?? '-' }), plan.rejectReason && (_jsx(Descriptions.Item, { label: "\u9A73\u56DE\u7406\u7531", span: 2, children: plan.rejectReason })), plan.companyConfirmUsername && (_jsxs(Descriptions.Item, { label: "\u4F01\u4E1A\u786E\u8BA4", span: 2, children: [plan.companyConfirmUsername, " \u00B7 ", formatTime(plan.companyConfirmTime)] })), plan.status >= 3 && plan.submitPersonnelFileName && (_jsx(Descriptions.Item, { label: "\u5BA1\u6838\u4EBA\u5458\u540D\u5355", span: 2, children: plan.submitPersonnelFileName })), plan.status >= 3 && plan.submitSchemeFileName && (_jsx(Descriptions.Item, { label: "\u65B9\u6848\u6587\u6863", span: 2, children: plan.submitSchemeFileName })), plan.status >= 3 && (_jsxs(Descriptions.Item, { label: "\u4F01\u4E1A\u56DE\u4F20", span: 2, children: [(() => {
                                                    const ready = !!(plan.enterpriseRosterFileId && plan.enterpriseShiftFileId);
                                                    if (plan.status === 4) {
                                                        return ready
                                                            ? _jsx(Tag, { color: "success", children: "\u5DF2\u786E\u8BA4 \u00B7 \u6750\u6599\u5DF2\u9F50" })
                                                            : _jsx(Tag, { color: "warning", children: "\u5DF2\u786E\u8BA4 \u00B7 \u5F85\u8865\u4EA4" });
                                                    }
                                                    if (ready)
                                                        return _jsx(Tag, { color: "processing", children: "\u6750\u6599\u5DF2\u9F50" });
                                                    if (plan.enterpriseRosterFileId || plan.enterpriseShiftFileId) {
                                                        return _jsx(Tag, { color: "warning", children: "\u90E8\u5206\u56DE\u4F20" });
                                                    }
                                                    return _jsx(Tag, { children: "\u672A\u56DE\u4F20" });
                                                })(), plan.enterpriseReturnTime ? (_jsxs(Typography.Text, { type: "secondary", style: { marginLeft: 8 }, children: [plan.enterpriseReturnUsername || '企业', " \u00B7 ", formatTime(plan.enterpriseReturnTime)] })) : null] })), plan.status >= 3 && (_jsx(Descriptions.Item, { label: "\u4F01\u4E1A\u7B5B\u9009\u540D\u5355", span: 2, children: plan.enterpriseRosterFileName || '未补交' })), plan.status >= 3 && (_jsx(Descriptions.Item, { label: "\u4F01\u4E1A\u6392\u73ED\u8868", span: 2, children: plan.enterpriseShiftFileName || '未补交' }))] }), isPlanAnalyzing(plan) && (_jsx(Progress, { percent: isPlanSyncing(plan) ? 100 : Math.round(plan.progressPct ?? 0), status: "active", format: isPlanSyncing(plan) ? () => '同步分析结果…' : undefined, style: { marginBottom: 16 } })), plan.status >= 2 && Number(plan.recommendedRows ?? 0) > 0 && (bundleItems?.total ?? 0) === 0 && (_jsx(Alert, { type: "info", showIcon: true, style: { marginBottom: 12 }, message: "\u6B63\u5728\u540C\u6B65\u5206\u6790\u7ED3\u679C", description: "\u4EBA\u624D\u8BC6\u522B\u5DF2\u5B8C\u6210\uFF0C\u660E\u7EC6\u6570\u636E\u6B63\u5728\u5199\u5165\uFF0C\u8BF7\u7A0D\u5019\u2026" })), plan.status >= 2 && detailBundleQuery.isError && (_jsx(Alert, { type: "error", showIcon: true, style: { marginBottom: 12 }, message: "\u52A0\u8F7D\u5206\u6790\u7ED3\u679C\u5931\u8D25", description: detailBundleQuery.error instanceof Error ? detailBundleQuery.error.message : '请稍后刷新重试' })), plan.status >= 2 && (_jsx("p", { style: { marginBottom: 12, color: '#666' }, children: plan.status === 2
                                        ? '分析结果已同步至本系统，可直接查看和修改人员明细；提交企业审核时需上传审核人员名单与方案文档，并固化当前已审核数据生成规划表。'
                                        : '已提交企业审核，企业可查看与下载提交时固化的已审核规划数据、人员名单及方案文档。' })), _jsx(Tabs, { items: [
                                        {
                                            key: 'distribution',
                                            label: '认定分布',
                                            disabled: plan.status < 2,
                                            children: (_jsx(Table, { rowKey: (r) => `${r.jobName}-${r.levelName}`, size: "small", loading: detailBundleQuery.isFetching && !bundleDistribution, columns: distributionColumns, dataSource: bundleDistribution ?? [], pagination: false, locale: { emptyText: plan.status < 2 ? '分析完成后可查看' : '暂无认定分布数据' } })),
                                        },
                                        {
                                            key: 'matrix',
                                            label: '矩阵汇总',
                                            disabled: plan.status < 2,
                                            children: (_jsx(Table, { rowKey: "id", size: "small", loading: detailBundleQuery.isFetching && !bundleMatrix, columns: matrixColumns, dataSource: bundleMatrix ?? [], pagination: false, locale: { emptyText: plan.status < 2 ? '分析完成后可查看' : '暂无矩阵数据' } })),
                                        },
                                        {
                                            key: 'items',
                                            label: '人员明细',
                                            disabled: plan.status < 2,
                                            children: (_jsxs(_Fragment, { children: [_jsx(Select, { allowClear: true, placeholder: "\u6309\u89C4\u5212\u5E74\u7B5B\u9009", style: { width: 140, marginBottom: 12 }, value: itemYearFilter, onChange: setItemYearFilter, options: [2026, 2027, 2028].map((y) => ({ value: y, label: `${y}年` })) }), _jsx(Table, { rowKey: (r) => String(r.id ?? r.rowIndex), size: "small", loading: detailBundleQuery.isFetching && !bundleItems, columns: itemColumns, dataSource: bundleItems?.items ?? [], scroll: { x: 900 }, pagination: {
                                                            current: itemPage,
                                                            pageSize: itemPageSize,
                                                            total: bundleItems?.total ?? 0,
                                                            onChange: (p, ps) => { setItemPage(p); setItemPageSize(ps); },
                                                        }, locale: { emptyText: plan.status < 2 ? '分析完成后可查看' : '暂无人员明细' } })] })),
                                        },
                                    ] }), plan.status >= 3 && (_jsx(Card, { size: "small", title: "\u5BA1\u6838\u4E0E\u63D0\u4EA4\u5386\u53F2", style: { marginTop: 16 }, children: _jsxs(Descriptions, { column: 1, size: "small", style: { marginBottom: 12 }, children: [_jsx(Descriptions.Item, { label: "\u9A73\u56DE\u5386\u53F2", children: renderRejectHistory(plan.rejectHistory) }), _jsx(Descriptions.Item, { label: "\u63D0\u4EA4\u5386\u53F2", children: renderSubmitHistory(plan.submitHistory) })] }) }))] })) }), _jsxs(Modal, { title: isRejectedPlan(plan) ? '重新提交企业审核' : '提交企业审核', open: submitOpen, onCancel: () => {
                            setSubmitOpen(false);
                            setSubmitPersonnelList([]);
                            setSubmitSchemeList([]);
                        }, onOk: handleSubmitEnterprise, confirmLoading: submitting, destroyOnClose: true, width: 560, okText: "\u786E\u8BA4\u63D0\u4EA4", children: [isRejectedPlan(plan) && (_jsx(Alert, { type: "warning", showIcon: true, style: { marginBottom: 16 }, message: "\u8BE5\u65B9\u6848\u66FE\u88AB\u4F01\u4E1A\u9A73\u56DE\uFF0C\u8BF7\u786E\u8BA4\u5DF2\u6839\u636E\u9A73\u56DE\u7406\u7531\u4FEE\u6539\u65B9\u6848\u540E\u518D\u63D0\u4EA4", description: plan?.rejectReason ? `上次驳回理由：${plan.rejectReason}` : undefined })), _jsx("p", { style: { marginBottom: 16, color: '#666' }, children: "\u63D0\u4EA4\u540E\u5C06\u56FA\u5316\u5F53\u524D\u5DF2\u5BA1\u6838\u6570\u636E\u5E76\u751F\u6210\u89C4\u5212\u8868\uFF0C\u540C\u65F6\u4E0A\u4F20\u4EE5\u4E0B\u6750\u6599\u4F9B\u4F01\u4E1A\u5BA1\u6838\u3002" }), _jsxs(Form, { layout: "vertical", children: [_jsx(Form.Item, { label: "\u4F01\u4E1A\u5BA1\u6838\u4EBA\u5458\u540D\u5355", required: true, extra: "\u652F\u6301 .xlsx / .xls", children: _jsx(Upload, { accept: ".xlsx,.xls", maxCount: 1, fileList: submitPersonnelList, beforeUpload: () => false, onChange: ({ fileList: fl }) => setSubmitPersonnelList(fl), children: _jsx(Button, { icon: _jsx(PlusOutlined, {}), children: "\u9009\u62E9\u4EBA\u5458\u540D\u5355 Excel" }) }) }), _jsx(Form.Item, { label: "\u65B9\u6848\u6587\u6863", required: true, extra: "\u652F\u6301 Excel\u3001Word \u6216 PDF", children: _jsx(Upload, { accept: ".xlsx,.xls,.doc,.docx,.pdf", maxCount: 1, fileList: submitSchemeList, beforeUpload: () => false, onChange: ({ fileList: fl }) => setSubmitSchemeList(fl), children: _jsx(Button, { icon: _jsx(PlusOutlined, {}), children: "\u9009\u62E9\u65B9\u6848\u6587\u6863" }) }) })] })] }), _jsx(Modal, { title: "\u4F01\u4E1A\u9A73\u56DE", open: rejectOpen, onCancel: () => setRejectOpen(false), onOk: () => {
                            if (!plan || !rejectReason.trim()) {
                                message.warning('请填写驳回理由');
                                return;
                            }
                            runAction(() => trainApi.companyRejectThreeYearPlan(plan.id, rejectReason.trim()), '已驳回').then(() => {
                                setRejectOpen(false);
                                setRejectReason('');
                            });
                        }, confirmLoading: actionLoading, children: _jsx(Input.TextArea, { rows: 4, value: rejectReason, onChange: (e) => setRejectReason(e.target.value), placeholder: "\u8BF7\u586B\u5199\u9A73\u56DE\u7406\u7531" }) }), _jsx(Modal, { title: "\u7F16\u8F91\u4EBA\u5458\u660E\u7EC6", open: editItemOpen, onCancel: () => {
                            setEditItemOpen(false);
                            setEditingItem(null);
                            itemForm.resetFields();
                        }, onOk: handleSaveItem, confirmLoading: savingItem, destroyOnClose: true, children: _jsxs(Form, { form: itemForm, layout: "vertical", children: [_jsx(Form.Item, { name: "personName", label: "\u59D3\u540D", rules: [{ required: true, message: '请输入姓名' }], children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "jobName", label: "\u5DE5\u79CD", rules: [{ required: true, message: '请输入工种' }], children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "levelName", label: "\u7B49\u7EA7", children: _jsx(Input, {}) }), _jsx(Form.Item, { name: "eligibleDate", label: "\u8BA4\u5B9A\u65F6\u95F4", children: _jsx(Input, { placeholder: "\u5982\uFF1A2026-03" }) }), _jsx(Form.Item, { name: "planYear", label: "\u89C4\u5212\u5E74", children: _jsx(InputNumber, { min: 2020, max: 2040, style: { width: '100%' } }) })] }) }), _jsx(Modal, { title: "\u4EBA\u624D\u8BC6\u522B\u5BA1\u6838\u5DE5\u4F5C\u53F0", open: reviewOpen, onCancel: () => setReviewOpen(false), footer: null, width: "92vw", style: { top: 24 }, destroyOnClose: true, children: plan?.rencaiTaskId && (_jsx("iframe", { ref: reviewIframeRef, title: "rencaishibie-review", src: `/rencai/tasks/${plan.rencaiTaskId}/review?embed=1`, style: { width: '100%', height: '75vh', border: 0 }, onLoad: sendReviewAuth })) })] })] }));
};

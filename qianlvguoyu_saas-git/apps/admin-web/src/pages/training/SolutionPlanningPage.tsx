import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Divider,
  Drawer,
  Dropdown,
  Flex,
  Form,
  Input,
  InputNumber,
  Modal,
  Progress,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Timeline,
  Typography,
  Upload,
  message,
} from 'antd';
import type { MenuProps } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { UploadFile } from 'antd/es/upload';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  CheckOutlined,
  CloseOutlined,
  CloudDownloadOutlined,
  CloudUploadOutlined,
  DeleteOutlined,
  DownOutlined,
  EditOutlined,
  EyeOutlined,
  PlusOutlined,
  ReloadOutlined,
  SendOutlined,
} from '@ant-design/icons';
import * as trainApi from '../../api/training';
import { useAuthStore } from '../../stores/authStore';
import type {
  DemandPublicData,
  ThreeYearPlan,
  ThreeYearPlanItem,
  ThreeYearPlanMatrix,
  ThreeYearPlanDistribution,
  ThreeYearPlanRejectHistoryItem,
  ThreeYearPlanSubmitHistoryItem,
} from '../../types/training';
import { PersonnelCertImportPanel } from './PersonnelCertImportPanel';
import { TrainingProcessSteps } from './TrainingProcessSteps';

const { Text } = Typography;

const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '草稿', color: 'default' },
  1: { label: '分析中', color: 'processing' },
  2: { label: '分析完成请审核', color: 'error' },
  3: { label: '待机构审核', color: 'warning' },
  4: { label: '企业已确认', color: 'success' },
  5: { label: '已驳回', color: 'error' },
  6: { label: '已作废', color: 'default' },
};

function formatTime(value?: string): string {
  if (!value) return '-';
  return value.replace('T', ' ').slice(0, 19);
}

function isRejectedPlan(plan: ThreeYearPlan | null): boolean {
  return !!plan && planStatusCode(plan.status) === 5;
}

function renderRejectHistory(items?: ThreeYearPlanRejectHistoryItem[]) {
  if (!items || items.length === 0) {
    return <Text type="secondary">暂无驳回历史</Text>;
  }
  return (
    <Timeline
      items={items
        .slice()
        .reverse()
        .map((item, idx) => ({
          key: idx,
          color: 'red',
          children: (
            <div>
              <div style={{ fontWeight: 600 }}>{item.reason || '（未填写理由）'}</div>
              <div style={{ fontSize: 12, color: '#999' }}>
                {item.rejectedByUsername || '企业'}
                {item.rejectedAt ? ` · ${formatTime(item.rejectedAt)}` : ''}
              </div>
            </div>
          ),
        }))}
    />
  );
}

function renderSubmitHistory(items?: ThreeYearPlanSubmitHistoryItem[]) {
  if (!items || items.length === 0) {
    return <Text type="secondary">暂无提交历史</Text>;
  }
  return (
    <Timeline
      items={items
        .slice()
        .reverse()
        .map((item, idx) => ({
          key: idx,
          color: 'blue',
          children: (
            <div>
              <div style={{ fontWeight: 600 }}>
                {item.submittedByUsername || '机构'}
                {item.submittedAt ? ` · ${formatTime(item.submittedAt)}` : ''}
              </div>
              {(item.personnelFileName || item.schemeFileName) && (
                <div style={{ fontSize: 12, color: '#999' }}>
                  {item.personnelFileName ? `人员名单：${item.personnelFileName}` : ''}
                  {item.personnelFileName && item.schemeFileName ? '；' : ''}
                  {item.schemeFileName ? `方案文档：${item.schemeFileName}` : ''}
                </div>
              )}
            </div>
          ),
        }))}
    />
  );
}

function planStatusCode(status: number | string | undefined): number {
  return Number(status ?? 0);
}

function isPlanAnalyzing(row: ThreeYearPlan): boolean {
  return planStatusCode(row.status) === 1;
}

function isPlanSyncing(row: ThreeYearPlan): boolean {
  if (!isPlanAnalyzing(row)) return false;
  if (row.analysisStatus === 'syncing') return true;
  const rencai = row.rencaiTaskStatus ?? '';
  return rencai === 'COMPLETED' || rencai === 'PENDING_MANUAL';
}

function isPlatformAdminUser(roles: string[] | undefined, username: string | undefined): boolean {
  const roleSet = new Set((roles ?? []).map((r) => String(r).toUpperCase()));
  return roleSet.has('PLATFORM_ADMIN') || roleSet.has('SUPER_ADMIN') || username === 'admin';
}

export const SolutionPlanningPage: React.FC = () => {
  const queryClient = useQueryClient();
  const loginRole = useAuthStore((s) => s.currentUser?.loginRole);
  const roles = useAuthStore((s) => s.currentUser?.roles ?? []);
  const username = useAuthStore((s) => s.currentUser?.username);
  const isPlatformAdmin = isPlatformAdminUser(roles, username);
  /** 企业账号仅查看/确认；平台超级管理员始终视为机构操作员 */
  const isEnterpriseViewer = loginRole === 'company' && !isPlatformAdmin;
  const canManagePlan = !isEnterpriseViewer;
  const [activeTab, setActiveTab] = useState('three-year');
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [statusFilter, setStatusFilter] = useState<number | undefined>();
  const [titleFilter, setTitleFilter] = useState('');

  const [createOpen, setCreateOpen] = useState(false);
  const [createForm] = Form.useForm();
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const [creating, setCreating] = useState(false);

  const [detailOpen, setDetailOpen] = useState(false);
  const [currentPlan, setCurrentPlan] = useState<ThreeYearPlan | null>(null);
  const [itemPage, setItemPage] = useState(1);
  const [itemPageSize, setItemPageSize] = useState(20);
  const [itemYearFilter, setItemYearFilter] = useState<number | undefined>();
  const [actionLoading, setActionLoading] = useState(false);
  const [rejectOpen, setRejectOpen] = useState(false);
  const [rejectReason, setRejectReason] = useState('');
  const [submitOpen, setSubmitOpen] = useState(false);
  const [submitPersonnelList, setSubmitPersonnelList] = useState<UploadFile[]>([]);
  const [submitSchemeList, setSubmitSchemeList] = useState<UploadFile[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [reviewOpen, setReviewOpen] = useState(false);
  const [editItemOpen, setEditItemOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<ThreeYearPlanItem | null>(null);
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
    queryFn: () => trainApi.pageResearchOrgs({ page: 1, pageSize: 200 }),
  });

  const detailQuery = useQuery({
    queryKey: ['three-year-plan', currentPlan?.id],
    queryFn: () => trainApi.getThreeYearPlan(currentPlan!.id),
    enabled: detailOpen && !!currentPlan?.id && Number(currentPlan?.status ?? 0) < 2,
    refetchInterval: (query) => {
      const plan = query.state.data;
      return plan?.status === 1 ? 3000 : false;
    },
  });

  /** 轮询或列表行状态 >=2 时即触发 bundle，不等待 currentPlan 更新 */
  const statusForBundleFetch = Number(
    detailQuery.data?.status ?? currentPlan?.status ?? 0,
  );

  const detailBundleQuery = useQuery({
    queryKey: ['three-year-plan-bundle', currentPlan?.id, itemPage, itemPageSize, itemYearFilter],
    queryFn: () => trainApi.getThreeYearPlanDetailBundle(currentPlan!.id, {
      page: itemPage,
      pageSize: itemPageSize,
      planYear: itemYearFilter,
    }),
    enabled: detailOpen && !!currentPlan?.id && statusForBundleFetch >= 2,
    staleTime: 30_000,
  });

  const resolvedPlanStatus = Number(
    detailBundleQuery.data?.plan?.status
    ?? detailQuery.data?.status
    ?? currentPlan?.status
    ?? 0,
  );

  const plan = detailBundleQuery.data?.plan ?? detailQuery.data ?? currentPlan;
  const bundleItems = detailBundleQuery.data?.items;
  const bundleMatrix = detailBundleQuery.data?.matrix;
  const bundleDistribution = detailBundleQuery.data?.distribution;
  const companyList = (publicDataQuery.data as DemandPublicData | undefined)?.company_list ?? [];
  const researchList = researchQuery.data?.items ?? [];

  const refreshList = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: ['three-year-plans'] });
  }, [queryClient]);

  const openDetail = (row: ThreeYearPlan) => {
    detailRetryCountRef.current = 0;
    setCurrentPlan(row);
    setItemPage(1);
    setItemYearFilter(undefined);
    setDetailOpen(true);
  };

  const closeDetail = useCallback((planId?: number) => {
    setDetailOpen(false);
    setCurrentPlan(null);
    setItemPage(1);
    setItemYearFilter(undefined);
    if (planId != null) {
      queryClient.removeQueries({ queryKey: ['three-year-plan', planId] });
      queryClient.removeQueries({ queryKey: ['three-year-plan-bundle', planId] });
    }
  }, [queryClient]);

  const invalidateDetailData = useCallback((planId: number) => {
    queryClient.invalidateQueries({ queryKey: ['three-year-plan', planId] });
    queryClient.invalidateQueries({ queryKey: ['three-year-plan-bundle', planId] });
  }, [queryClient]);

  const openEditItem = (item: ThreeYearPlanItem) => {
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
    if (!plan?.id || !editingItem?.id) return;
    try {
      const values = await itemForm.validateFields();
      setSavingItem(true);
      await trainApi.updateThreeYearPlanItem(plan.id, editingItem.id, values);
      message.success('已保存');
      setEditItemOpen(false);
      setEditingItem(null);
      itemForm.resetFields();
      invalidateDetailData(plan.id);
    } catch (e) {
      if (e instanceof Error && e.message) {
        message.error(e.message);
      }
    } finally {
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
    } catch (e) {
      if (e instanceof Error && e.message) {
        message.error(e.message);
      }
    } finally {
      setCreating(false);
    }
  };

  const runAction = async (
    fn: () => Promise<unknown>,
    successMsg: string,
    options?: { refreshDetail?: boolean },
  ) => {
    setActionLoading(true);
    try {
      await fn();
      message.success(successMsg);
      refreshList();
      if (options?.refreshDetail !== false && detailOpen && currentPlan?.id) {
        invalidateDetailData(currentPlan.id);
      }
    } catch (e) {
      message.error(e instanceof Error ? e.message : '操作失败');
    } finally {
      setActionLoading(false);
    }
  };

  const handleDeletePlan = async (planId: number) => {
    setActionLoading(true);
    try {
      await trainApi.deleteThreeYearPlan(planId);
      closeDetail(planId);
      message.success('已删除');
      refreshList();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '操作失败');
    } finally {
      setActionLoading(false);
    }
  };

  const handleSubmitEnterprise = async () => {
    if (!plan) return;
    const personnelFile = submitPersonnelList[0]?.originFileObj as File | undefined;
    const schemeFile = submitSchemeList[0]?.originFileObj as File | undefined;
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
    } catch (e) {
      message.error(e instanceof Error ? e.message : '提交失败');
    } finally {
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
    if (
      resolvedPlanStatus >= 2
      && recommended > 0
      && itemTotal === 0
      && !detailBundleQuery.isFetching
      && detailRetryCountRef.current < 10
    ) {
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

  const columns: ColumnsType<ThreeYearPlan> = [
    { title: '规划编号', dataIndex: 'planNo', width: 180 },
    { title: '标题', dataIndex: 'title', ellipsis: true },
    { title: '目标企业', dataIndex: 'targetOrgName', width: 160, ellipsis: true },
    {
      title: '状态',
      dataIndex: 'status',
      width: 150,
      render: (status: number, row) => {
        const code = planStatusCode(status);
        const meta = STATUS_MAP[code] ?? { label: row.statusLabel ?? '未知', color: 'default' };
        const label = row.statusLabel ?? meta.label;
        const color = isPlanSyncing(row) ? 'processing' : meta.color;
        return <Tag color={color}>{label}</Tag>;
      },
    },
    {
      title: '企业回传',
      width: 140,
      render: (_, row) => {
        const code = planStatusCode(row.status);
        if (code < 3) return '-';
        const ready = !!(row.enterpriseRosterFileId && row.enterpriseShiftFileId);
        if (code === 4) {
          return ready
            ? <Tag color="success">已确认 · 材料已齐</Tag>
            : <Tag color="warning">已确认 · 待补交</Tag>;
        }
        if (ready) return <Tag color="processing">材料已齐</Tag>;
        if (row.enterpriseRosterFileId || row.enterpriseShiftFileId) {
          return <Tag color="warning">部分回传</Tag>;
        }
        return <Tag>未回传</Tag>;
      },
    },
    {
      title: '分析进度',
      width: 140,
      render: (_, row) => {
        if (!isPlanAnalyzing(row)) return '-';
        if (isPlanSyncing(row)) {
          return <Progress percent={100} size="small" status="active" format={() => '同步中'} />;
        }
        const pct = Math.round(row.progressPct ?? 0);
        return <Progress percent={pct} size="small" status={pct >= 99 ? 'active' : undefined} />;
      },
    },
    { title: '创建时间', dataIndex: 'createTime', width: 170, render: formatTime },
    {
      title: '操作',
      width: 100,
      fixed: 'right',
      render: (_, row) => (
        <Button type="link" icon={<EyeOutlined />} onClick={() => openDetail(row)}>
          详情
        </Button>
      ),
    },
  ];

  const itemColumns: ColumnsType<ThreeYearPlanItem> = [
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
        fixed: 'right' as const,
        render: (_: unknown, row: ThreeYearPlanItem) => (
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            disabled={!row.id}
            onClick={() => openEditItem(row)}
          >
            编辑
          </Button>
        ),
      }]
      : []),
  ];

  const distributionColumns: ColumnsType<ThreeYearPlanDistribution> = [
    { title: '工种', dataIndex: 'jobName', width: 140 },
    { title: '等级', dataIndex: 'levelName', width: 90 },
    { title: '人数', dataIndex: 'employeeCount', width: 70 },
    { title: '开班阈值', dataIndex: 'thresholdMin', width: 90 },
    {
      title: '达阈值',
      dataIndex: 'meetsThreshold',
      width: 80,
      render: (v: boolean) => (v ? <Tag color="success">是</Tag> : <Tag color="warning">否</Tag>),
    },
    { title: '提示', dataIndex: 'warning', ellipsis: true },
  ];

  const matrixColumns: ColumnsType<ThreeYearPlanMatrix> = [
    { title: '工种', dataIndex: 'jobName' },
    { title: '规划年', dataIndex: 'planYear', width: 90 },
    { title: '等级', dataIndex: 'levelName', width: 100 },
    { title: '人数', dataIndex: 'headcount', width: 80 },
  ];

  const renderDetailToolbar = () => {
    if (!plan) return null;

    const workflowActions: React.ReactNode[] = [];
    const downloadMenuItems: MenuProps['items'] = [];

    if (planStatusCode(plan.status) === 1) {
      workflowActions.push(
        <Button
          key="refresh"
          icon={<ReloadOutlined />}
          loading={actionLoading}
          onClick={() => runAction(
            () => trainApi.refreshThreeYearPlanAnalysis(plan.id),
            '已刷新分析状态',
          )}
        >
          刷新进度
        </Button>,
      );
    }

    if (plan.status >= 2 && plan.rencaiTaskId) {
      workflowActions.push(
        <Button key="review" onClick={() => setReviewOpen(true)}>
          审核工作台
        </Button>,
      );
    }

    if (canManagePlan && (plan.status === 2 || plan.status === 5)) {
      workflowActions.push(
        <Button
          key="submit"
          type="primary"
          icon={<SendOutlined />}
          onClick={() => setSubmitOpen(true)}
        >
          {plan.status === 5 ? '重新提交企业审核' : '提交企业审核'}
        </Button>,
      );
    }

    if (isEnterpriseViewer && plan.status === 3) {
      const returnReady = !!(plan.enterpriseRosterFileId && plan.enterpriseShiftFileId);
      const missingReturn: string[] = [];
      if (!plan.enterpriseRosterFileId) missingReturn.push('筛选后培训名单');
      if (!plan.enterpriseShiftFileId) missingReturn.push('排班表');
      const doConfirm = () => runAction(
        () => trainApi.companyConfirmThreeYearPlan(plan.id),
        '已确认',
      );
      workflowActions.push(
        <Button
          key="confirm"
          type="primary"
          icon={<CheckOutlined />}
          loading={actionLoading}
          onClick={() => {
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
          }}
        >
          企业确认
        </Button>,
        <Button
          key="reject"
          danger
          icon={<CloseOutlined />}
          onClick={() => setRejectOpen(true)}
        >
          企业驳回
        </Button>,
      );
    }

    if (plan.status >= 2) {
      downloadMenuItems.push({
        key: 'reviewed',
        icon: <CloudDownloadOutlined />,
        label: '已审核规划表',
        onClick: () => trainApi.downloadThreeYearPlanFile(plan.id, 'reviewed'),
      });
    }

    if (plan.status >= 2 && plan.rencaiTaskId) {
      downloadMenuItems.push(
        {
          key: 'plan',
          icon: <CloudDownloadOutlined />,
          label: '原始规划表',
          onClick: () => trainApi.downloadThreeYearPlanFile(plan.id, 'plan'),
        },
        {
          key: 'result',
          icon: <CloudDownloadOutlined />,
          label: '原始认定结果',
          onClick: () => trainApi.downloadThreeYearPlanFile(plan.id, 'result'),
        },
      );
    }

    if (plan.status >= 3 && plan.submitPersonnelFileId) {
      downloadMenuItems.push({
        key: 'submit-personnel',
        icon: <CloudDownloadOutlined />,
        label: '审核人员名单',
        onClick: () => trainApi.downloadThreeYearPlanFile(plan.id, 'submit-personnel'),
      });
    }

    if (plan.status >= 3 && plan.submitSchemeFileId) {
      downloadMenuItems.push({
        key: 'submit-scheme',
        icon: <CloudDownloadOutlined />,
        label: '方案文档',
        onClick: () => trainApi.downloadThreeYearPlanFile(plan.id, 'submit-scheme'),
      });
    }

    if (plan.status >= 3 && plan.enterpriseRosterFileId) {
      downloadMenuItems.push({
        key: 'enterprise-roster',
        icon: <CloudDownloadOutlined />,
        label: '企业筛选培训名单',
        onClick: () => trainApi.downloadThreeYearPlanFile(plan.id, 'enterprise-roster'),
      });
    }

    if (plan.status >= 3 && plan.enterpriseShiftFileId) {
      downloadMenuItems.push({
        key: 'enterprise-shift',
        icon: <CloudDownloadOutlined />,
        label: '企业排班表',
        onClick: () => trainApi.downloadThreeYearPlanFile(plan.id, 'enterprise-shift'),
      });
    }

    const hasDownloads = (downloadMenuItems?.length ?? 0) > 0;
    const showDelete = canManagePlan && plan.status !== 3;

    if (!workflowActions.length && !hasDownloads && !showDelete) {
      return null;
    }

    return (
      <Flex wrap="wrap" gap={8} align="center" justify="flex-end">
        {workflowActions.length > 0 && (
          <Space wrap size={8}>
            {workflowActions}
          </Space>
        )}
        {workflowActions.length > 0 && (hasDownloads || showDelete) && (
          <Divider type="vertical" style={{ margin: 0, height: 20 }} />
        )}
        {hasDownloads && (
          <Dropdown menu={{ items: downloadMenuItems }}>
            <Button icon={<CloudDownloadOutlined />}>
              下载文件 <DownOutlined />
            </Button>
          </Dropdown>
        )}
        {showDelete && (
          <Button
            danger
            icon={<DeleteOutlined />}
            loading={actionLoading}
            onClick={() => {
              Modal.confirm({
                title: '确认删除该三年规划？',
                onOk: () => handleDeletePlan(plan.id),
              });
            }}
          >
            删除
          </Button>
        )}
      </Flex>
    );
  };

  return (
    <>
      <TrainingProcessSteps current={1} />
    <Card bordered={false} bodyStyle={{ padding: 0 }}>
      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        type="card"
        style={{ padding: '16px 16px 0' }}
        items={[
          {
            key: 'three-year',
            label: '三年规划',
            children: (
              <div style={{ padding: 16 }}>
                <Space style={{ marginBottom: 16 }} wrap>
                  <Input
                    placeholder="搜索标题"
                    allowClear
                    style={{ width: 200 }}
                    value={titleFilter}
                    onChange={(e) => setTitleFilter(e.target.value)}
                    onPressEnter={() => { setPage(1); refreshList(); }}
                  />
                  <Select
                    allowClear
                    placeholder="状态筛选"
                    style={{ width: 160 }}
                    value={statusFilter}
                    onChange={setStatusFilter}
                    options={Object.entries(STATUS_MAP).map(([k, v]) => ({
                      value: Number(k),
                      label: v.label,
                    }))}
                  />
                  <Button icon={<ReloadOutlined />} onClick={refreshList}>刷新</Button>
                  {canManagePlan && (
                    <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
                      新建三年规划
                    </Button>
                  )}
                </Space>

                <Table<ThreeYearPlan>
                  rowKey="id"
                  loading={listQuery.isLoading}
                  columns={columns}
                  dataSource={listQuery.data?.items ?? []}
                  scroll={{ x: 1100 }}
                  pagination={{
                    current: page,
                    pageSize,
                    total: listQuery.data?.total ?? 0,
                    showSizeChanger: true,
                    onChange: (p, ps) => { setPage(p); setPageSize(ps); },
                  }}
                />
              </div>
            ),
          },
          {
            key: 'training-plan',
            label: '人员认定',
            children: <PersonnelCertImportPanel />,
          },
        ]}
      />

      <Modal
        title="新建三年规划"
        open={createOpen}
        onCancel={() => setCreateOpen(false)}
        onOk={handleCreate}
        confirmLoading={creating}
        destroyOnClose
        width={560}
      >
        <Form form={createForm} layout="vertical">
          <Form.Item name="title" label="规划标题" rules={[{ required: true, message: '请输入标题' }]}>
            <Input placeholder="如：某某企业 2026-2028 技能认定三年规划" />
          </Form.Item>
          <Form.Item name="targetOrgId" label="目标企业" rules={[{ required: true, message: '请选择企业' }]}>
            <Select
              showSearch
              placeholder="选择企业"
              optionFilterProp="label"
              options={companyList.map((c) => ({ value: c.id, label: c.title }))}
            />
          </Form.Item>
          <Form.Item name="researchOrgId" label="关联企业调研（可选）">
            <Select
              allowClear
              showSearch
              placeholder="选择调研记录"
              optionFilterProp="label"
              options={researchList.map((r) => ({ value: r.id, label: r.title }))}
            />
          </Form.Item>
          <Form.Item label="员工 Excel" required>
            <Upload
              accept=".xlsx,.xls"
              maxCount={1}
              fileList={fileList}
              beforeUpload={() => false}
              onChange={({ fileList: fl }) => setFileList(fl)}
            >
              <Button>选择 Excel 文件</Button>
            </Upload>
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title={plan ? (
          <Text ellipsis={{ tooltip: plan.title }} style={{ maxWidth: '100%' }}>
            三年规划：{plan.title}
          </Text>
        ) : '三年规划详情'}
        width={960}
        open={detailOpen}
        onClose={() => closeDetail(currentPlan?.id)}
      >
        {plan && (
          <>
            {(() => {
              const toolbar = renderDetailToolbar();
              if (!toolbar) return null;
              return (
                <div
                  style={{
                    margin: '-24px -24px 16px',
                    padding: '12px 24px',
                    background: '#fafafa',
                    borderBottom: '1px solid #f0f0f0',
                  }}
                >
                  {toolbar}
                </div>
              );
            })()}
            {isRejectedPlan(plan) && plan.rejectReason && (
              <Alert
                type="error"
                showIcon
                style={{ marginBottom: 16 }}
                message="该方案已被企业驳回"
                description={
                  <div>
                    <p style={{ marginBottom: 4, color: '#666' }}>驳回理由：</p>
                    <p style={{ marginBottom: 8, whiteSpace: 'pre-wrap' }}>{plan.rejectReason}</p>
                    <p style={{ marginBottom: 0, color: '#999', fontSize: 12 }}>
                      请根据驳回理由修改方案信息后，点击「重新提交企业审核」
                    </p>
                  </div>
                }
              />
            )}
            {plan.status === 4 && !(plan.enterpriseRosterFileId && plan.enterpriseShiftFileId) && (
              <Alert
                type="warning"
                showIcon
                style={{ marginBottom: 16 }}
                message="已确认 · 待补交回传材料"
                description={
                  [
                    !plan.enterpriseRosterFileId ? '筛选后培训名单' : null,
                    !plan.enterpriseShiftFileId ? '排班表' : null,
                  ].filter(Boolean).join('、')
                  + '尚未上传。请尽快补交，补齐前不建议进入分班/排课。'
                }
              />
            )}
            {(plan.status === 3 || plan.status === 4) && (canManagePlan || isEnterpriseViewer) && (
              <Alert
                type="info"
                showIcon
                style={{ marginBottom: 16 }}
                message={plan.status === 4 ? '回传材料补交 / 覆盖' : '企业回传材料上传'}
                description={(
                  <Space wrap>
                    <Upload
                      accept=".xlsx,.xls"
                      showUploadList={false}
                      beforeUpload={(file) => {
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
                          } catch (e) {
                            message.error(e instanceof Error ? e.message : '上传失败');
                          } finally {
                            setActionLoading(false);
                          }
                        })();
                        return false;
                      }}
                    >
                      <Button size="small" icon={<CloudUploadOutlined />} loading={actionLoading}>
                        {plan.enterpriseRosterFileId
                          ? (plan.status === 4 ? '覆盖名单' : '重新上传名单')
                          : (plan.status === 4 ? '补交名单' : '上传名单')}
                      </Button>
                    </Upload>
                    <Upload
                      accept=".xlsx,.xls"
                      showUploadList={false}
                      beforeUpload={(file) => {
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
                          } catch (e) {
                            message.error(e instanceof Error ? e.message : '上传失败');
                          } finally {
                            setActionLoading(false);
                          }
                        })();
                        return false;
                      }}
                    >
                      <Button size="small" icon={<CloudUploadOutlined />} loading={actionLoading}>
                        {plan.enterpriseShiftFileId
                          ? (plan.status === 4 ? '覆盖排班表' : '重新上传排班表')
                          : (plan.status === 4 ? '补交排班表' : '上传排班表')}
                      </Button>
                    </Upload>
                  </Space>
                )}
              />
            )}
            <Descriptions column={2} size="small" bordered style={{ marginBottom: 16 }}>
              <Descriptions.Item label="规划编号">{plan.planNo}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={STATUS_MAP[plan.status]?.color}>
                  {plan.statusLabel ?? STATUS_MAP[plan.status]?.label}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="目标企业">{plan.targetOrgName}</Descriptions.Item>
              <Descriptions.Item label="规划年份">{plan.planYears}</Descriptions.Item>
              <Descriptions.Item label="人才识别任务">{plan.rencaiTaskId ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="推荐人数">{plan.recommendedRows ?? '-'}</Descriptions.Item>
              {plan.rejectReason && (
                <Descriptions.Item label="驳回理由" span={2}>{plan.rejectReason}</Descriptions.Item>
              )}
              {plan.companyConfirmUsername && (
                <Descriptions.Item label="企业确认" span={2}>
                  {plan.companyConfirmUsername} · {formatTime(plan.companyConfirmTime)}
                </Descriptions.Item>
              )}
              {plan.status >= 3 && plan.submitPersonnelFileName && (
                <Descriptions.Item label="审核人员名单" span={2}>
                  {plan.submitPersonnelFileName}
                </Descriptions.Item>
              )}
              {plan.status >= 3 && plan.submitSchemeFileName && (
                <Descriptions.Item label="方案文档" span={2}>
                  {plan.submitSchemeFileName}
                </Descriptions.Item>
              )}
              {plan.status >= 3 && (
                <Descriptions.Item label="企业回传" span={2}>
                  {(() => {
                    const ready = !!(plan.enterpriseRosterFileId && plan.enterpriseShiftFileId);
                    if (plan.status === 4) {
                      return ready
                        ? <Tag color="success">已确认 · 材料已齐</Tag>
                        : <Tag color="warning">已确认 · 待补交</Tag>;
                    }
                    if (ready) return <Tag color="processing">材料已齐</Tag>;
                    if (plan.enterpriseRosterFileId || plan.enterpriseShiftFileId) {
                      return <Tag color="warning">部分回传</Tag>;
                    }
                    return <Tag>未回传</Tag>;
                  })()}
                  {plan.enterpriseReturnTime ? (
                    <Typography.Text type="secondary" style={{ marginLeft: 8 }}>
                      {plan.enterpriseReturnUsername || '企业'} · {formatTime(plan.enterpriseReturnTime)}
                    </Typography.Text>
                  ) : null}
                </Descriptions.Item>
              )}
              {plan.status >= 3 && (
                <Descriptions.Item label="企业筛选名单" span={2}>
                  {plan.enterpriseRosterFileName || '未补交'}
                </Descriptions.Item>
              )}
              {plan.status >= 3 && (
                <Descriptions.Item label="企业排班表" span={2}>
                  {plan.enterpriseShiftFileName || '未补交'}
                </Descriptions.Item>
              )}
            </Descriptions>

            {isPlanAnalyzing(plan) && (
              <Progress
                percent={isPlanSyncing(plan) ? 100 : Math.round(plan.progressPct ?? 0)}
                status="active"
                format={isPlanSyncing(plan) ? () => '同步分析结果…' : undefined}
                style={{ marginBottom: 16 }}
              />
            )}

            {plan.status >= 2 && Number(plan.recommendedRows ?? 0) > 0 && (bundleItems?.total ?? 0) === 0 && (
              <Alert
                type="info"
                showIcon
                style={{ marginBottom: 12 }}
                message="正在同步分析结果"
                description="人才识别已完成，明细数据正在写入，请稍候…"
              />
            )}

            {plan.status >= 2 && detailBundleQuery.isError && (
              <Alert
                type="error"
                showIcon
                style={{ marginBottom: 12 }}
                message="加载分析结果失败"
                description={detailBundleQuery.error instanceof Error ? detailBundleQuery.error.message : '请稍后刷新重试'}
              />
            )}

            {plan.status >= 2 && (
              <p style={{ marginBottom: 12, color: '#666' }}>
                {plan.status === 2
                  ? '分析结果已同步至本系统，可直接查看和修改人员明细；提交企业审核时需上传审核人员名单与方案文档，并固化当前已审核数据生成规划表。'
                  : '已提交企业审核，企业可查看与下载提交时固化的已审核规划数据、人员名单及方案文档。'}
              </p>
            )}

            <Tabs
              items={[
                {
                  key: 'distribution',
                  label: '认定分布',
                  disabled: plan.status < 2,
                  children: (
                    <Table<ThreeYearPlanDistribution>
                      rowKey={(r) => `${r.jobName}-${r.levelName}`}
                      size="small"
                      loading={detailBundleQuery.isFetching && !bundleDistribution}
                      columns={distributionColumns}
                      dataSource={bundleDistribution ?? []}
                      pagination={false}
                      locale={{ emptyText: plan.status < 2 ? '分析完成后可查看' : '暂无认定分布数据' }}
                    />
                  ),
                },
                {
                  key: 'matrix',
                  label: '矩阵汇总',
                  disabled: plan.status < 2,
                  children: (
                    <Table<ThreeYearPlanMatrix>
                      rowKey="id"
                      size="small"
                      loading={detailBundleQuery.isFetching && !bundleMatrix}
                      columns={matrixColumns}
                      dataSource={bundleMatrix ?? []}
                      pagination={false}
                      locale={{ emptyText: plan.status < 2 ? '分析完成后可查看' : '暂无矩阵数据' }}
                    />
                  ),
                },
                {
                  key: 'items',
                  label: '人员明细',
                  disabled: plan.status < 2,
                  children: (
                    <>
                      <Select
                        allowClear
                        placeholder="按规划年筛选"
                        style={{ width: 140, marginBottom: 12 }}
                        value={itemYearFilter}
                        onChange={setItemYearFilter}
                        options={[2026, 2027, 2028].map((y) => ({ value: y, label: `${y}年` }))}
                      />
                      <Table<ThreeYearPlanItem>
                        rowKey={(r) => String(r.id ?? r.rowIndex)}
                        size="small"
                        loading={detailBundleQuery.isFetching && !bundleItems}
                        columns={itemColumns}
                        dataSource={bundleItems?.items ?? []}
                        scroll={{ x: 900 }}
                        pagination={{
                          current: itemPage,
                          pageSize: itemPageSize,
                          total: bundleItems?.total ?? 0,
                          onChange: (p, ps) => { setItemPage(p); setItemPageSize(ps); },
                        }}
                        locale={{ emptyText: plan.status < 2 ? '分析完成后可查看' : '暂无人员明细' }}
                      />
                    </>
                  ),
                },
              ]}
            />

            {plan.status >= 3 && (
              <Card size="small" title="审核与提交历史" style={{ marginTop: 16 }}>
                <Descriptions column={1} size="small" style={{ marginBottom: 12 }}>
                  <Descriptions.Item label="驳回历史">
                    {renderRejectHistory(plan.rejectHistory)}
                  </Descriptions.Item>
                  <Descriptions.Item label="提交历史">
                    {renderSubmitHistory(plan.submitHistory)}
                  </Descriptions.Item>
                </Descriptions>
              </Card>
            )}
          </>
        )}
      </Drawer>

      <Modal
        title={isRejectedPlan(plan) ? '重新提交企业审核' : '提交企业审核'}
        open={submitOpen}
        onCancel={() => {
          setSubmitOpen(false);
          setSubmitPersonnelList([]);
          setSubmitSchemeList([]);
        }}
        onOk={handleSubmitEnterprise}
        confirmLoading={submitting}
        destroyOnClose
        width={560}
        okText="确认提交"
      >
        {isRejectedPlan(plan) && (
          <Alert
            type="warning"
            showIcon
            style={{ marginBottom: 16 }}
            message="该方案曾被企业驳回，请确认已根据驳回理由修改方案后再提交"
            description={plan?.rejectReason ? `上次驳回理由：${plan.rejectReason}` : undefined}
          />
        )}
        <p style={{ marginBottom: 16, color: '#666' }}>
          提交后将固化当前已审核数据并生成规划表，同时上传以下材料供企业审核。
        </p>
        <Form layout="vertical">
          <Form.Item label="企业审核人员名单" required extra="支持 .xlsx / .xls">
            <Upload
              accept=".xlsx,.xls"
              maxCount={1}
              fileList={submitPersonnelList}
              beforeUpload={() => false}
              onChange={({ fileList: fl }) => setSubmitPersonnelList(fl)}
            >
              <Button icon={<PlusOutlined />}>选择人员名单 Excel</Button>
            </Upload>
          </Form.Item>
          <Form.Item label="方案文档" required extra="支持 Excel、Word 或 PDF">
            <Upload
              accept=".xlsx,.xls,.doc,.docx,.pdf"
              maxCount={1}
              fileList={submitSchemeList}
              beforeUpload={() => false}
              onChange={({ fileList: fl }) => setSubmitSchemeList(fl)}
            >
              <Button icon={<PlusOutlined />}>选择方案文档</Button>
            </Upload>
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="企业驳回"
        open={rejectOpen}
        onCancel={() => setRejectOpen(false)}
        onOk={() => {
          if (!plan || !rejectReason.trim()) {
            message.warning('请填写驳回理由');
            return;
          }
          runAction(
            () => trainApi.companyRejectThreeYearPlan(plan.id, rejectReason.trim()),
            '已驳回',
          ).then(() => {
            setRejectOpen(false);
            setRejectReason('');
          });
        }}
        confirmLoading={actionLoading}
      >
        <Input.TextArea
          rows={4}
          value={rejectReason}
          onChange={(e) => setRejectReason(e.target.value)}
          placeholder="请填写驳回理由"
        />
      </Modal>

      <Modal
        title="编辑人员明细"
        open={editItemOpen}
        onCancel={() => {
          setEditItemOpen(false);
          setEditingItem(null);
          itemForm.resetFields();
        }}
        onOk={handleSaveItem}
        confirmLoading={savingItem}
        destroyOnClose
      >
        <Form form={itemForm} layout="vertical">
          <Form.Item name="personName" label="姓名" rules={[{ required: true, message: '请输入姓名' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="jobName" label="工种" rules={[{ required: true, message: '请输入工种' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="levelName" label="等级">
            <Input />
          </Form.Item>
          <Form.Item name="eligibleDate" label="认定时间">
            <Input placeholder="如：2026-03" />
          </Form.Item>
          <Form.Item name="planYear" label="规划年">
            <InputNumber min={2020} max={2040} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="人才识别审核工作台"
        open={reviewOpen}
        onCancel={() => setReviewOpen(false)}
        footer={null}
        width="92vw"
        style={{ top: 24 }}
        destroyOnClose
      >
        {plan?.rencaiTaskId && (
          <iframe
            title="rencaishibie-review"
            src={`/rencai/tasks/${plan.rencaiTaskId}/review?embed=1`}
            style={{ width: '100%', height: '75vh', border: 0 }}
          />
        )}
      </Modal>
    </Card>
    </>
  );
};

import { useQuery } from '@tanstack/react-query';
import {
  Alert,
  Button,
  Card,
  Col,
  Empty,
  Input,
  Progress,
  Result,
  Row,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  BankOutlined,
  DashboardOutlined,
  ReloadOutlined,
  WarningOutlined,
} from '@ant-design/icons';
import ReactECharts from 'echarts-for-react';
import { useEffect, useMemo, useState } from 'react';
import * as trainApi from '../../api/training';
import { PermissionGate } from '../../directives/permission';
import type {
  DashboardAlertItem,
  DashboardCompanyOverview,
  DashboardCompanyTrainingItem,
  DashboardMilestoneItem,
  DashboardMonthlyTrend,
  DashboardStageDistribution,
} from '../../types/training';
import './TrainingDashboardPage.css';

const STAGE_COLORS: Record<string, string> = {
  待开班: '#faad14',
  培训中: '#1890ff',
  已结业: '#52c41a',
  pending: '#faad14',
  in_progress: '#1890ff',
  reviewing: '#722ed1',
  completed: '#52c41a',
};

function resolveColor(stage: string): string {
  return STAGE_COLORS[stage] ?? '#8c8c8c';
}

function resolveErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : '数据加载失败，请稍后重试';
}

function resolveStatusTag(status: string) {
  const normalized = status?.toLowerCase() ?? '';
  if (normalized === 'danger' || normalized === 'error' || normalized === 'critical' || status === '预警') {
    return <Tag color="red">预警</Tag>;
  }
  if (normalized === 'warning' || status === '关注') {
    return <Tag color="orange">注意</Tag>;
  }
  if (normalized === 'success' || normalized === 'normal' || normalized === 'ok' || status === '正常') {
    return <Tag color="green">正常</Tag>;
  }
  return <Tag>{status || '未知'}</Tag>;
}

function formatRate(value?: number) {
  const n = Number(value ?? 0);
  return Number.isFinite(n) ? n.toFixed(1) : '0.0';
}

function stageTag(stage: string) {
  const color = stage === '已结业' ? 'green' : stage === '培训中' ? 'blue' : 'gold';
  return <Tag color={color}>{stage || '待开班'}</Tag>;
}

const LIVE_INTERVAL_MS = 10_000;

const liveQueryOptions = {
  refetchInterval: LIVE_INTERVAL_MS,
  refetchIntervalInBackground: false,
  refetchOnWindowFocus: true,
  staleTime: 0,
} as const;

export function TrainingDashboardPage() {
  const [companyKeyword, setCompanyKeyword] = useState('');
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, []);

  const dashboardQuery = useQuery({
    queryKey: ['training-dashboard'],
    queryFn: () => trainApi.getDashboard(),
    ...liveQueryOptions,
  });

  const milestonesQuery = useQuery<DashboardMilestoneItem[]>({
    queryKey: ['training-dashboard-milestones'],
    queryFn: () => trainApi.getDashboardMilestones(),
    ...liveQueryOptions,
  });

  const stageQuery = useQuery<DashboardStageDistribution>({
    queryKey: ['training-dashboard-stage'],
    queryFn: () => trainApi.getDashboardStageDistribution(),
    ...liveQueryOptions,
  });

  const handleRefresh = () => {
    void dashboardQuery.refetch();
    void milestonesQuery.refetch();
    void stageQuery.refetch();
  };

  const partialErrors: string[] = [];
  if (milestonesQuery.isError) {
    partialErrors.push(`项目里程碑：${resolveErrorMessage(milestonesQuery.error)}`);
  }
  if (stageQuery.isError) {
    partialErrors.push(`阶段分布：${resolveErrorMessage(stageQuery.error)}`);
  }

  const data = dashboardQuery.data;
  const summary = data?.summary;
  const monthlyTrends = data?.monthlyTrends ?? [];
  const alerts = data?.alerts ?? [];
  const modules = data?.modules ?? [];
  const companies = data?.companies ?? [];

  const filteredCompanies = useMemo(() => {
    const kw = companyKeyword.trim().toLowerCase();
    const list = kw
      ? companies.filter((c) => (c.companyName || '').toLowerCase().includes(kw))
      : [...companies];
    const rank = (c: DashboardCompanyOverview) => {
      if (c.inProgressCount > 0 || c.activeClassCount > 0) return 0;
      if (c.trainingCount > 0 || c.studentCount > 0 || c.classCount > 0) return 1;
      return 2;
    };
    return [...list].sort((a, b) => {
      const byRank = rank(a) - rank(b);
      if (byRank !== 0) return byRank;
      if (b.inProgressCount !== a.inProgressCount) return b.inProgressCount - a.inProgressCount;
      if (b.studentCount !== a.studentCount) return b.studentCount - a.studentCount;
      if (b.trainingCount !== a.trainingCount) return b.trainingCount - a.trainingCount;
      return (a.companyName || '').localeCompare(b.companyName || '', 'zh');
    });
  }, [companies, companyKeyword]);

  const trendOption = useMemo(() => ({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 16, top: 24, bottom: 32 },
    xAxis: {
      type: 'category',
      data: monthlyTrends.map((m: DashboardMonthlyTrend) => m.month),
      axisLine: { lineStyle: { color: '#d9d9d9' } },
    },
    yAxis: { type: 'value', name: '方案数', minInterval: 1 },
    series: [
      {
        name: '新增方案',
        type: 'line',
        smooth: true,
        data: monthlyTrends.map((m: DashboardMonthlyTrend) => m.count),
        areaStyle: { color: 'rgba(47,107,255,0.16)' },
        lineStyle: { color: '#2f6bff' },
        itemStyle: { color: '#2f6bff' },
      },
    ],
  }), [monthlyTrends]);

  const stageEntries = useMemo(
    () => (stageQuery.isError ? [] : Object.entries(stageQuery.data ?? {})),
    [stageQuery.data, stageQuery.isError],
  );

  const stageOption = useMemo(() => ({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['50%', '44%'],
        data: stageEntries.map(([name, value]) => ({ name, value })),
        label: { formatter: '{b}\n{c}项' },
        color: stageEntries.map(([name]) => resolveColor(name)),
      },
    ],
  }), [stageEntries]);

  const companyBarOption = useMemo(() => {
    const top = [...filteredCompanies]
      .sort((a, b) => b.studentCount - a.studentCount)
      .slice(0, 10)
      .reverse();
    return {
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: 16, right: 36, top: 12, bottom: 24, containLabel: true },
      xAxis: { type: 'value', name: '学员数', minInterval: 1 },
      yAxis: {
        type: 'category',
        data: top.map((c) => c.companyName),
        axisLabel: { width: 100, overflow: 'truncate' },
      },
      series: [{
        type: 'bar',
        data: top.map((c) => c.studentCount),
        itemStyle: { color: '#2f6bff', borderRadius: [0, 5, 5, 0] },
        label: { show: true, position: 'right' },
      }],
    };
  }, [filteredCompanies]);

  const alertColumns: ColumnsType<DashboardAlertItem> = [
    { title: '指标', dataIndex: 'indicator', width: 140 },
    { title: '当前值', dataIndex: 'currentValue', width: 100 },
    { title: '阈值', dataIndex: 'threshold', width: 100 },
    { title: '趋势', dataIndex: 'trend', width: 100 },
    { title: '状态', dataIndex: 'status', width: 90, render: resolveStatusTag },
    { title: '建议', dataIndex: 'suggestion', ellipsis: true },
  ];

  const milestoneColumns: ColumnsType<DashboardMilestoneItem> = [
    { title: '项目/方案', dataIndex: 'projectName', ellipsis: true },
    { title: '所属企业', dataIndex: 'companyName', width: 150, ellipsis: true },
    { title: '当前阶段', dataIndex: 'currentStage', width: 100 },
    {
      title: '进度',
      dataIndex: 'progress',
      width: 140,
      render: (v: number) => (
        <Progress
          percent={v ?? 0}
          size="small"
          status={v >= 100 ? 'success' : v >= 40 ? 'active' : 'normal'}
        />
      ),
    },
  ];

  const companyColumns: ColumnsType<DashboardCompanyOverview> = [
    {
      title: '企业名称',
      dataIndex: 'companyName',
      ellipsis: true,
      render: (name: string, row) => (
        <Space>
          <BankOutlined style={{ color: '#2f6bff' }} />
          <span>{name}</span>
          {row.trainingCount === 0 && row.projectCount === 0 ? <Tag>未开班</Tag> : null}
        </Space>
      ),
    },
    { title: '方案', dataIndex: 'projectCount', width: 70, align: 'right' },
    { title: '开班', dataIndex: 'trainingCount', width: 70, align: 'right' },
    {
      title: '阶段',
      width: 180,
      render: (_, row) => (
        <Space size={4} wrap>
          <Tag color="gold">待{row.pendingCount}</Tag>
          <Tag color="blue">培{row.inProgressCount}</Tag>
          <Tag color="green">结{row.completedCount}</Tag>
        </Space>
      ),
    },
    { title: '班级', dataIndex: 'classCount', width: 70, align: 'right' },
    { title: '在培班', dataIndex: 'activeClassCount', width: 80, align: 'right' },
    { title: '学员', dataIndex: 'studentCount', width: 70, align: 'right' },
    { title: '考试', dataIndex: 'examCount', width: 70, align: 'right' },
    { title: '证书', dataIndex: 'issuedCertCount', width: 70, align: 'right' },
    { title: '补贴', dataIndex: 'subsidyCount', width: 70, align: 'right' },
  ];

  const trainingColumns: ColumnsType<DashboardCompanyTrainingItem> = [
    { title: '培训名称', dataIndex: 'title', ellipsis: true },
    { title: '阶段', dataIndex: 'stage', width: 90, render: (v: string) => stageTag(v) },
    {
      title: '培训周期',
      width: 180,
      render: (_, row) => {
        const start = row.startDate || '-';
        const end = row.endDate || '-';
        return `${start} ~ ${end}`;
      },
    },
    { title: '班级', dataIndex: 'classCount', width: 70, align: 'right' },
    { title: '学员', dataIndex: 'studentCount', width: 70, align: 'right' },
    { title: '考试', dataIndex: 'examCount', width: 70, align: 'right' },
    { title: '证书', dataIndex: 'issuedCertCount', width: 70, align: 'right' },
  ];

  const rateCards = summary
    ? [
        { title: '计划执行率', value: summary.planExecutionRate, foot: '已结业方案 / 已立项方案' },
        { title: '培训完成率', value: summary.trainingCompletionRate, foot: '已结业开班 / 全部开班' },
        { title: '资料完整率', value: summary.materialSubmissionRate, foot: '已关联学员 / 花名册' },
        { title: '证书发放率', value: summary.certificationRate, foot: '已发证书 / 学员数' },
        { title: '补贴审批率', value: summary.subsidyApprovalRate, foot: '已通过预算 / 预算总数' },
      ]
    : [];

  const renderBody = () => {
    if (dashboardQuery.isLoading && !dashboardQuery.data) {
      return <Spin size="large" style={{ display: 'block', margin: 120 }} />;
    }

    if (dashboardQuery.isError) {
      return (
        <Result
          status="error"
          title="培训数据面板加载失败"
          subTitle={resolveErrorMessage(dashboardQuery.error)}
          extra={
            <Button type="primary" onClick={handleRefresh}>
              重试
            </Button>
          }
        />
      );
    }

    if (!summary) {
      return <Empty description="暂无看板数据" />;
    }

    return (
      <>
        {partialErrors.length > 0 && (
          <Alert
            type="warning"
            showIcon
            message="部分数据加载失败"
            description={
              <ul style={{ margin: 0, paddingLeft: 20 }}>
                {partialErrors.map((item) => (
                  <li key={item}>{item}</li>
                ))}
              </ul>
            }
            action={
              <Button size="small" onClick={handleRefresh}>
                重试
              </Button>
            }
          />
        )}

        <div className="training-dashboard-kpis">
          {rateCards.map((k) => (
            <div className="training-dashboard-kpi" key={k.title}>
              <div className="training-dashboard-kpi-label">{k.title}</div>
              <div className="training-dashboard-kpi-value">{formatRate(k.value)}%</div>
              <div className="training-dashboard-kpi-foot">{k.foot}</div>
            </div>
          ))}
        </div>

        <Card title="培训全流程模块概览" size="small">
          <div className="training-dashboard-modules">
            {(modules.length > 0
              ? modules
              : [
                  { code: 'projects', name: '方案立项', count: summary.totalProjects, hint: '已立项方案' },
                  { code: 'students', name: '学员花名册', count: summary.totalStudents, hint: '在册学员' },
                  { code: 'certs', name: '资质证书', count: summary.totalCertificates, hint: '论文/专利等' },
                  { code: 'budgets', name: '预算条目', count: summary.totalBudgets, hint: '预算总数' },
                ]
            ).map((m) => (
              <div className="training-dashboard-module" key={m.code}>
                <div className="training-dashboard-module-title">
                  <span>{m.name}</span>
                </div>
                <div className="training-dashboard-module-value">{m.count}</div>
                <div className="training-dashboard-module-hint">{m.hint}</div>
              </div>
            ))}
          </div>
        </Card>

        <Row gutter={16}>
          <Col xs={12} sm={8} md={6} lg={4}>
            <Card size="small"><Typography.Text type="secondary">企业总数</Typography.Text><div style={{ fontSize: 22, fontWeight: 700 }}>{summary.totalCompanies ?? companies.length}</div></Card>
          </Col>
          <Col xs={12} sm={8} md={6} lg={4}>
            <Card size="small"><Typography.Text type="secondary">参训企业</Typography.Text><div style={{ fontSize: 22, fontWeight: 700 }}>{summary.activeCompanies ?? 0}</div></Card>
          </Col>
          <Col xs={12} sm={8} md={6} lg={4}>
            <Card size="small"><Typography.Text type="secondary">在培开班</Typography.Text><div style={{ fontSize: 22, fontWeight: 700 }}>{summary.inProgressTrainings ?? 0}</div></Card>
          </Col>
          <Col xs={12} sm={8} md={6} lg={4}>
            <Card size="small"><Typography.Text type="secondary">班级总数</Typography.Text><div style={{ fontSize: 22, fontWeight: 700 }}>{summary.totalClasses ?? 0}</div></Card>
          </Col>
          <Col xs={12} sm={8} md={6} lg={4}>
            <Card size="small"><Typography.Text type="secondary">学员总数</Typography.Text><div style={{ fontSize: 22, fontWeight: 700 }}>{summary.totalStudents ?? 0}</div></Card>
          </Col>
          <Col xs={12} sm={8} md={6} lg={4}>
            <Card size="small"><Typography.Text type="secondary">已发证书</Typography.Text><div style={{ fontSize: 22, fontWeight: 700 }}>{summary.totalIssuedCerts ?? 0}</div></Card>
          </Col>
        </Row>

        <Card
          title={
            <Space>
              <BankOutlined />
              企业培训明细
              <Typography.Text type="secondary">共 {filteredCompanies.length} 家</Typography.Text>
            </Space>
          }
          extra={
            <Input.Search
              allowClear
              placeholder="搜索企业"
              style={{ width: 220 }}
              onSearch={setCompanyKeyword}
              onChange={(e) => {
                if (!e.target.value) setCompanyKeyword('');
              }}
            />
          }
        >
          <Table
            rowKey={(r) => String(r.companyId)}
            size="middle"
            columns={companyColumns}
            dataSource={filteredCompanies}
            pagination={{ pageSize: 10, showSizeChanger: true, showTotal: (t) => `共 ${t} 家企业` }}
            locale={{ emptyText: '暂无企业培训数据' }}
            expandable={{
              expandedRowRender: (row) =>
                row.trainings?.length ? (
                  <Table
                    rowKey={(r) => String(r.trainInfoId)}
                    size="small"
                    columns={trainingColumns}
                    dataSource={row.trainings}
                    pagination={false}
                  />
                ) : (
                  <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="该企业暂无开班培训" />
                ),
              rowExpandable: () => true,
            }}
          />
        </Card>

        <div className="training-dashboard-chart-grid">
          <Card title="近 6 个月方案立项趋势">
            {monthlyTrends.length > 0 ? (
              <ReactECharts option={trendOption} className="training-dashboard-chart" />
            ) : (
              <Empty description="暂无趋势数据" />
            )}
          </Card>
          <Card title="开班阶段分布" loading={stageQuery.isLoading}>
            {stageQuery.isError ? (
              <Empty description={resolveErrorMessage(stageQuery.error)} />
            ) : stageEntries.length > 0 ? (
              <ReactECharts option={stageOption} className="training-dashboard-chart" />
            ) : (
              <Empty description="暂无阶段数据" />
            )}
          </Card>
        </div>

        <Card title="企业学员规模 Top10">
          {filteredCompanies.some((c) => c.studentCount > 0) ? (
            <ReactECharts option={companyBarOption} className="training-dashboard-chart" />
          ) : (
            <Empty description="暂无企业学员数据" />
          )}
        </Card>

        <Row gutter={16}>
          <Col span={14}>
            <Card
              title={
                <Space>
                  <WarningOutlined style={{ color: '#fa8c16' }} />
                  风险预警
                </Space>
              }
            >
              <Table
                rowKey={(_, i) => `${_.indicator}-${i}`}
                size="middle"
                columns={alertColumns}
                dataSource={alerts}
                pagination={false}
                locale={{ emptyText: '暂无预警' }}
              />
            </Card>
          </Col>
          <Col span={10}>
            <Card title="项目里程碑" loading={milestonesQuery.isLoading}>
              {milestonesQuery.isError ? (
                <Empty description={resolveErrorMessage(milestonesQuery.error)} />
              ) : (
                <Table
                  rowKey={(r) => String(r.projectId)}
                  size="middle"
                  columns={milestoneColumns}
                  dataSource={milestonesQuery.data ?? []}
                  pagination={false}
                  scroll={{ y: 320 }}
                  locale={{ emptyText: '暂无进行中的项目' }}
                />
              )}
            </Card>
          </Col>
        </Row>
      </>
    );
  };

  return (
    <PermissionGate code="train:dashboard:view" fallback={<Empty description="暂无培训数据面板访问权限" />}>
      <div className="training-dashboard">
        <section className="page-heading">
          <div className="page-title-line">
            <div className="page-title-icon">
              <DashboardOutlined />
            </div>
            <div>
              <Typography.Title level={4} style={{ margin: 0 }}>
                培训管理数据驾驶舱
              </Typography.Title>
              <Typography.Text type="secondary">
                覆盖企业、方案立项、开班跟班、考试发证、补贴申报，页面打开时每 10 秒自动刷新
              </Typography.Text>
            </div>
          </div>
          <Space>
            <span className="training-dashboard-live">
              <span className="training-dashboard-live-dot" />
              实时
              {dashboardQuery.dataUpdatedAt
                ? ` · ${Math.max(0, Math.floor((now - dashboardQuery.dataUpdatedAt) / 1000))} 秒前更新`
                : ''}
            </span>
            <Button
              type="link"
              icon={<ReloadOutlined />}
              onClick={handleRefresh}
            >
              立即刷新
            </Button>
          </Space>
        </section>
        {renderBody()}
      </div>
    </PermissionGate>
  );
}

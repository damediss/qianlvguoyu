import { useQuery } from '@tanstack/react-query';
import { Alert, Button, Card, Col, Empty, Result, Row, Space, Spin, Statistic, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  DollarOutlined,
  FileTextOutlined,
  ProjectOutlined,
  ReloadOutlined,
  RiseOutlined,
  TeamOutlined,
  WarningOutlined,
} from '@ant-design/icons';
import ReactECharts from 'echarts-for-react';
import * as trainApi from '../../api/training';
import { PermissionGate } from '../../directives/permission';
import type {
  DashboardAlertItem,
  DashboardMilestoneItem,
  DashboardMonthlyTrend,
  DashboardStageDistribution,
} from '../../types/training';
import './TrainingDashboardPage.css';

const STAGE_COLORS: Record<string, string> = {
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
  if (normalized === 'danger' || normalized === 'error' || normalized === 'critical') {
    return <Tag color="red">预警</Tag>;
  }
  if (normalized === 'warning') {
    return <Tag color="orange">注意</Tag>;
  }
  if (normalized === 'success' || normalized === 'normal' || normalized === 'ok') {
    return <Tag color="green">正常</Tag>;
  }
  return <Tag>{status || '未知'}</Tag>;
}

export function TrainingDashboardPage() {
  const dashboardQuery = useQuery({
    queryKey: ['training-dashboard'],
    queryFn: () => trainApi.getDashboard(),
    refetchInterval: 60_000,
  });

  const milestonesQuery = useQuery<DashboardMilestoneItem[]>({
    queryKey: ['training-dashboard-milestones'],
    queryFn: () => trainApi.getDashboardMilestones(),
  });

  const stageQuery = useQuery<DashboardStageDistribution>({
    queryKey: ['training-dashboard-stage'],
    queryFn: () => trainApi.getDashboardStageDistribution(),
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

  const renderBody = () => {
    if (dashboardQuery.isLoading && !dashboardQuery.data) {
      return <Spin size="large" style={{ display: 'block', margin: 120 }} />;
    }

    if (dashboardQuery.isError) {
      return (
        <Result
          status="error"
          title="培训数据看板加载失败"
          subTitle={resolveErrorMessage(dashboardQuery.error)}
          extra={
            <Button type="primary" onClick={handleRefresh}>
              重试
            </Button>
          }
        />
      );
    }

    const data = dashboardQuery.data;
    if (!data) {
      return <Empty description="暂无看板数据" />;
    }

    const { summary, monthlyTrends = [], alerts = [] } = data;

    const trendOption = {
      tooltip: { trigger: 'axis' },
      grid: { left: 40, right: 16, top: 24, bottom: 32 },
      xAxis: {
        type: 'category',
        data: monthlyTrends.map((m: DashboardMonthlyTrend) => m.month),
        axisLine: { lineStyle: { color: '#d9d9d9' } },
      },
      yAxis: { type: 'value' },
      series: [
        {
          name: '项目数',
          type: 'line',
          smooth: true,
          data: monthlyTrends.map((m: DashboardMonthlyTrend) => m.count),
          areaStyle: { color: 'rgba(24,144,255,0.18)' },
          lineStyle: { color: '#1890ff' },
          itemStyle: { color: '#1890ff' },
        },
      ],
    };

    const stageEntries = stageQuery.isError ? [] : Object.entries(stageQuery.data ?? {});
    const stageOption = {
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [
        {
          type: 'pie',
          radius: ['45%', '70%'],
          data: stageEntries.map(([name, value]) => ({ name, value })),
          label: { formatter: '{b}: {c}' },
          color: stageEntries.map(([name]) => resolveColor(name)),
        },
      ],
    };

    const kpiCards = [
      { title: '计划执行率', value: summary.planExecutionRate, suffix: '%', icon: <RiseOutlined />, color: '#52c41a' },
      { title: '培训完成率', value: summary.trainingCompletionRate, suffix: '%', icon: <ProjectOutlined />, color: '#13c2c2' },
      { title: '资料提交率', value: summary.materialSubmissionRate, suffix: '%', icon: <FileTextOutlined />, color: '#1890ff' },
      { title: '证书发放率', value: summary.certificationRate, suffix: '%', icon: <FileTextOutlined />, color: '#722ed1' },
      { title: '补贴审批率', value: summary.subsidyApprovalRate, suffix: '%', icon: <DollarOutlined />, color: '#fa8c16' },
    ];

    const alertColumns: ColumnsType<DashboardAlertItem> = [
      { title: '指标', dataIndex: 'indicator', width: 140 },
      { title: '当前值', dataIndex: 'currentValue', width: 120 },
      { title: '阈值', dataIndex: 'threshold', width: 120 },
      { title: '趋势', dataIndex: 'trend', width: 100 },
      { title: '状态', dataIndex: 'status', width: 100, render: resolveStatusTag },
      { title: '建议', dataIndex: 'suggestion' },
    ];

    const milestoneColumns: ColumnsType<DashboardMilestoneItem> = [
      { title: '项目', dataIndex: 'projectName', ellipsis: true },
      { title: '所属企业', dataIndex: 'companyName', width: 160, ellipsis: true },
      { title: '当前阶段', dataIndex: 'currentStage', width: 140 },
      {
        title: '进度',
        dataIndex: 'progress',
        width: 160,
        render: (v: number) => <Tag color={v >= 80 ? 'green' : v >= 40 ? 'blue' : 'orange'}>{v ?? 0}%</Tag>,
      },
    ];

    return (
      <>
        {partialErrors.length > 0 && (
          <Alert
            type="warning"
            showIcon
            style={{ marginBottom: 16 }}
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

        <Row gutter={16} style={{ marginBottom: 16 }}>
          {kpiCards.map((k) => (
            <Col xs={24} sm={12} md={8} lg={6} xl={Math.floor(24 / kpiCards.length) || 6} key={k.title}>
              <Card>
                <Statistic
                  title={k.title}
                  value={k.value}
                  suffix={k.suffix}
                  precision={2}
                  prefix={<span style={{ color: k.color }}>{k.icon}</span>}
                />
              </Card>
            </Col>
          ))}
        </Row>

        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={6}>
            <Card>
              <Statistic title="总项目数" value={summary.totalProjects} prefix={<ProjectOutlined />} />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic title="总学员数" value={summary.totalStudents} prefix={<TeamOutlined />} />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic title="总证书数" value={summary.totalCertificates} prefix={<FileTextOutlined />} />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic title="总预算数" value={summary.totalBudgets} prefix={<DollarOutlined />} />
            </Card>
          </Col>
        </Row>

        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={16}>
            <Card title="月度培训项目数趋势">
              <ReactECharts option={trendOption} style={{ height: 320 }} />
            </Card>
          </Col>
          <Col span={8}>
            <Card title="项目阶段分布" loading={stageQuery.isLoading}>
              {stageQuery.isError ? (
                <Empty description={resolveErrorMessage(stageQuery.error)} />
              ) : stageEntries.length > 0 ? (
                <ReactECharts option={stageOption} style={{ height: 320 }} />
              ) : (
                <Typography.Text type="secondary">暂无阶段数据</Typography.Text>
              )}
            </Card>
          </Col>
        </Row>

        <Row gutter={16} style={{ marginBottom: 16 }}>
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
    <PermissionGate code="train:dashboard:view" fallback={<Empty description="暂无培训数据看板访问权限" />}>
      <div className="training-dashboard">
        <section className="page-heading">
          <div>
            <Typography.Title level={4}>培训数据看板</Typography.Title>
            <Typography.Text type="secondary">
              培训计划执行、学员、证书、补贴等核心指标全景（自动每 60s 刷新）
            </Typography.Text>
          </div>
          <Space>
            <Button
              type="link"
              icon={<ReloadOutlined />}
              loading={dashboardQuery.isFetching || milestonesQuery.isFetching || stageQuery.isFetching}
              onClick={handleRefresh}
            >
              刷新
            </Button>
          </Space>
        </section>
        {renderBody()}
      </div>
    </PermissionGate>
  );
}

import {
  AlertOutlined,
  BarChartOutlined,
  CheckCircleOutlined,
  DashboardOutlined,
  FileDoneOutlined,
  ReloadOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import {
  Button,
  Card,
  DatePicker,
  Empty,
  Form,
  Progress,
  Select,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import dayjs, { type Dayjs } from 'dayjs';
import ReactECharts from 'echarts-for-react';
import { useMemo, useState } from 'react';
import { getCertificationDashboard } from '../../api/certificationDashboard';
import { PermissionGate } from '../../directives/permission';
import type {
  CertificationDashboardAlert,
  CertificationDashboardQuery,
} from '../../types/certificationDashboard';
import './CertificationDataPanelPage.css';

const phaseOptions = [
  { label: '任务创建阶段', value: 'task_creation' },
  { label: '认定筹备阶段', value: 'preparation' },
  { label: '认定归档阶段', value: 'archive' },
  { label: '认定完成', value: 'certification_completed' },
];

const statusOptions = [
  { label: '进行中', value: 'in_progress' },
  { label: '待审核', value: 'pending_review' },
  { label: '已驳回', value: 'rejected' },
  { label: '已完成', value: 'completed' },
];

const dateModeOptions = [
  { label: '按创建日期', value: 'created' },
  { label: '按考试日期', value: 'exam' },
  { label: '按归档日期', value: 'archive' },
];

const phaseColors = ['#2f6bff', '#7c5cff', '#f59e0b', '#10b981'];

interface FilterFormValues {
  dateRange?: [Dayjs, Dayjs];
  dateMode?: CertificationDashboardQuery['dateMode'];
  currentPhase?: string;
  status?: string;
  certificationType?: string;
  workTypeName?: string;
  levelName?: string;
}

export function CertificationDataPanelPage() {
  const [form] = Form.useForm<FilterFormValues>();
  const [query, setQuery] = useState<CertificationDashboardQuery>(() => {
    const endDate = dayjs();
    return {
      startDate: endDate.subtract(11, 'month').startOf('month').format('YYYY-MM-DD'),
      endDate: endDate.format('YYYY-MM-DD'),
      dateMode: 'created',
    };
  });

  const dashboardQuery = useQuery({
    queryKey: ['certification-dashboard', query],
    queryFn: () => getCertificationDashboard(query),
  });
  const dashboard = dashboardQuery.data;

  const applyFilters = (values: FilterFormValues) => {
    setQuery({
      startDate: values.dateRange?.[0]?.format('YYYY-MM-DD'),
      endDate: values.dateRange?.[1]?.format('YYYY-MM-DD'),
      dateMode: values.dateMode || 'created',
      currentPhase: values.currentPhase,
      status: values.status,
      certificationType: values.certificationType,
      workTypeName: values.workTypeName,
      levelName: values.levelName,
    });
  };

  const resetFilters = () => {
    const endDate = dayjs();
    const dateRange: [Dayjs, Dayjs] = [endDate.subtract(11, 'month').startOf('month'), endDate];
    form.setFieldsValue({ dateRange, dateMode: 'created' });
    setQuery({
      startDate: dateRange[0].format('YYYY-MM-DD'),
      endDate: dateRange[1].format('YYYY-MM-DD'),
      dateMode: 'created',
    });
  };

  const phaseOption = useMemo(() => ({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    color: phaseColors,
    series: [{
      type: 'pie',
      radius: ['42%', '68%'],
      center: ['50%', '44%'],
      label: { formatter: '{b}\n{c}项' },
      data: (dashboard?.phaseDistribution || []).map((item) => ({
        name: item.name,
        value: item.taskCount,
      })),
    }],
  }), [dashboard?.phaseDistribution]);

  const workTypeOption = useMemo(() => {
    const values = (dashboard?.workTypeDistribution || []).slice(0, 10).reverse();
    return {
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: 16, right: 36, top: 12, bottom: 24, containLabel: true },
      xAxis: { type: 'value', name: '人数' },
      yAxis: { type: 'category', data: values.map((item) => item.name) },
      series: [{
        type: 'bar',
        data: values.map((item) => item.peopleCount),
        itemStyle: { color: '#2f6bff', borderRadius: [0, 5, 5, 0] },
        label: { show: true, position: 'right' },
      }],
    };
  }, [dashboard?.workTypeDistribution]);

  const workTypeLevelOption = useMemo(() => {
    const source = dashboard?.workTypeLevelDistribution || [];
    const workTypes = [...new Set(source.map((item) => item.workTypeName))].slice(0, 10);
    const levels = [...new Set(source.map((item) => item.levelName))];
    return {
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend: { bottom: 0 },
      grid: { left: 16, right: 24, top: 16, bottom: 56, containLabel: true },
      xAxis: { type: 'category', data: workTypes, axisLabel: { rotate: 20 } },
      yAxis: { type: 'value', name: '人数' },
      series: levels.map((level) => ({
        name: level,
        type: 'bar',
        stack: 'people',
        data: workTypes.map((workType) => (
          source.find((item) => item.workTypeName === workType && item.levelName === level)?.peopleCount || 0
        )),
      })),
    };
  }, [dashboard?.workTypeLevelDistribution]);

  const trendOption = useMemo(() => ({
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0 },
    grid: { left: 16, right: 24, top: 16, bottom: 56, containLabel: true },
    xAxis: {
      type: 'category',
      data: (dashboard?.monthlyTrends || []).map((item) => item.month),
    },
    yAxis: [
      { type: 'value', name: '任务数' },
      { type: 'value', name: '归档人数' },
    ],
    series: [
      {
        name: '新增任务',
        type: 'bar',
        data: (dashboard?.monthlyTrends || []).map((item) => item.createdTaskCount),
        itemStyle: { color: '#93b4ff' },
      },
      {
        name: '归档任务',
        type: 'line',
        smooth: true,
        data: (dashboard?.monthlyTrends || []).map((item) => item.archivedTaskCount),
        itemStyle: { color: '#10b981' },
      },
      {
        name: '归档人数',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: (dashboard?.monthlyTrends || []).map((item) => item.archivedPeopleCount),
        itemStyle: { color: '#f59e0b' },
      },
    ],
  }), [dashboard?.monthlyTrends]);

  const alertColumns: ColumnsType<CertificationDashboardAlert> = [
    { title: '任务编号', dataIndex: 'taskNo', width: 150, render: (value) => value || '-' },
    { title: '任务名称', dataIndex: 'taskName', width: 220, ellipsis: true, render: (value) => value || '-' },
    {
      title: '级别',
      dataIndex: 'level',
      width: 90,
      render: (value) => <Tag color={value === 'error' || value === 'high' ? 'error' : 'warning'}>{value === 'error' || value === 'high' ? '高' : '关注'}</Tag>,
    },
    { title: '提醒内容', dataIndex: 'description', ellipsis: true },
    { title: '计划日期', dataIndex: 'dueDate', width: 120, render: (value) => value || '-' },
  ];

  return (
    <PermissionGate code="certification.dashboard.view" fallback={<Empty description="暂无认定数据面板访问权限" />}>
      <div className="knowledge-page cert-dashboard">
        <div className="page-heading knowledge-heading">
          <div className="page-title-line">
            <div className="page-title-icon"><DashboardOutlined /></div>
            <div>
              <Typography.Title level={1}>认定数据面板</Typography.Title>
              <Typography.Text type="secondary">
                汇总认定任务、对象、资源、风险与归档合规数据
              </Typography.Text>
            </div>
          </div>
          <Button
            icon={<ReloadOutlined />}
            loading={dashboardQuery.isFetching}
            onClick={() => dashboardQuery.refetch()}
          >
            刷新
          </Button>
        </div>

        <Card className="dashboard-filter-card" title="统计筛选" size="small">
          <Form
            form={form}
            layout="inline"
            initialValues={{
              dateRange: [dayjs(query.startDate), dayjs(query.endDate)],
              dateMode: 'created',
            }}
            onFinish={applyFilters}
          >
            <Form.Item name="dateRange" label="日期范围">
              <DatePicker.RangePicker allowClear={false} />
            </Form.Item>
            <Form.Item name="dateMode" label="日期口径">
              <Select style={{ width: 130 }} options={dateModeOptions} />
            </Form.Item>
            <Form.Item name="currentPhase" label="阶段">
              <Select allowClear style={{ width: 150 }} options={phaseOptions} />
            </Form.Item>
            <Form.Item name="status" label="状态">
              <Select allowClear style={{ width: 120 }} options={statusOptions} />
            </Form.Item>
            <Form.Item name="certificationType" label="认定类型">
              <Select
                allowClear
                style={{ width: 150 }}
                options={(dashboard?.filterOptions.certificationTypes || []).map((value) => ({ label: value, value }))}
              />
            </Form.Item>
            <Form.Item name="workTypeName" label="工种">
              <Select
                allowClear
                showSearch
                style={{ width: 150 }}
                options={(dashboard?.filterOptions.workTypes || []).map((value) => ({ label: value, value }))}
              />
            </Form.Item>
            <Form.Item name="levelName" label="等级">
              <Select
                allowClear
                style={{ width: 120 }}
                options={(dashboard?.filterOptions.levels || []).map((value) => ({ label: value, value }))}
              />
            </Form.Item>
            <Form.Item>
              <Space>
                <Button type="primary" htmlType="submit">查询</Button>
                <Button onClick={resetFilters}>重置</Button>
              </Space>
            </Form.Item>
          </Form>
        </Card>

        <div className="cert-dashboard-kpis">
          <KpiCard label="认定任务总数" value={dashboard?.summary.totalTasks} foot={`进行中 ${dashboard?.summary.inProgressTasks || 0} 项`} />
          <KpiCard label="已完成任务" value={dashboard?.summary.completedTasks} foot={`完成率 ${dashboard?.summary.taskCompletionRate || 0}%`} />
          <KpiCard label="计划认定人数" value={dashboard?.summary.plannedPeopleCount} foot={`实际 ${dashboard?.summary.actualPeopleCount || 0} 人`} />
          <KpiCard label="逾期任务" value={dashboard?.summary.overdueTasks} foot={`平均周期 ${dashboard?.summary.averageCycleDays || 0} 天`} danger />
          <KpiCard label="已归档任务" value={dashboard?.summary.archivedTasks} foot={`待归档 ${dashboard?.summary.pendingArchiveTasks || 0} 项`} />
          <KpiCard label="归档完成率" value={`${dashboard?.summary.archiveCompletionRate || 0}%`} foot={`材料完整率 ${dashboard?.summary.taskMaterialCompletionRate || 0}%`} />
        </div>

        <div className="cert-dashboard-chart-grid">
          <Card title="任务阶段分布" size="small">
            <ReactECharts option={phaseOption} className="cert-dashboard-chart" />
          </Card>
          <Card title="工种认定人数排名" size="small">
            <ReactECharts option={workTypeOption} className="cert-dashboard-chart" />
          </Card>
          <Card title="工种 × 等级人数" size="small">
            <ReactECharts option={workTypeLevelOption} className="cert-dashboard-chart" />
          </Card>
          <Card title="月度任务与归档趋势" size="small">
            <ReactECharts option={trendOption} className="cert-dashboard-chart" />
          </Card>
        </div>

        <Card title={<Space><TeamOutlined />资源状态</Space>} size="small">
          <div className="cert-dashboard-resource-grid">
            {(dashboard?.resources || []).map((resource) => (
              <div key={resource.code} className="cert-dashboard-resource">
                <div className="cert-dashboard-resource-title">
                  <span>{resource.name}</span>
                  <Tag color={resource.available > 0 ? 'success' : 'default'}>可用 {resource.available}</Tag>
                </div>
                <div className="cert-dashboard-resource-total">{resource.total}</div>
                <Typography.Text type="secondary">资源总量</Typography.Text>
                <div className="cert-dashboard-resource-meta">
                  <span>占用/非空闲：{resource.busy}</span>
                  <span>停用：{resource.disabled}</span>
                  <span>30天内到期：{resource.expiring}</span>
                  <span>{resource.capacity > 0 ? `可用容量：${resource.capacity}` : '容量：-'}</span>
                </div>
              </div>
            ))}
          </div>
        </Card>

        <Card title={<Space><CheckCircleOutlined />质量与合规</Space>} size="small">
          <div className="cert-dashboard-resource-grid">
            <QualityCard label="检查清单完成率" value={dashboard?.quality.checklistCompletionRate || 0} />
            <QualityCard label="考生材料完整率" value={dashboard?.quality.candidateMaterialCompletionRate || 0} />
            <KpiCard label="未关闭风险" value={dashboard?.quality.openRiskCount} foot={`高风险任务 ${dashboard?.quality.highRiskTaskCount || 0} 项`} danger />
            <KpiCard label="导入异常" value={dashboard?.quality.importErrorCount} foot="考生与归档材料导入异常" danger />
          </div>
        </Card>

        <div className="cert-dashboard-chart-grid">
          <Card title={<Space><AlertOutlined />风险任务</Space>} size="small">
            <Table
              rowKey={(record) => `${record.taskId}-${record.alertType}-${record.description}`}
              columns={alertColumns}
              dataSource={dashboard?.riskAlerts || []}
              loading={dashboardQuery.isLoading}
              pagination={false}
              size="small"
              scroll={{ x: 760 }}
            />
          </Card>
          <Card title={<Space><FileDoneOutlined />归档与合规提醒</Space>} size="small">
            <Table
              rowKey={(record) => `${record.taskId}-${record.alertType}-${record.description}`}
              columns={alertColumns}
              dataSource={dashboard?.complianceAlerts || []}
              loading={dashboardQuery.isLoading}
              pagination={false}
              size="small"
              scroll={{ x: 760 }}
            />
          </Card>
        </div>
      </div>
    </PermissionGate>
  );
}

function KpiCard({
  label,
  value,
  foot,
  danger = false,
}: {
  label: string;
  value?: number | string;
  foot: string;
  danger?: boolean;
}) {
  return (
    <div className="cert-dashboard-kpi">
      <div className="cert-dashboard-kpi-label">{label}</div>
      <div className="cert-dashboard-kpi-value" style={danger ? { color: '#dc2626' } : undefined}>
        {value ?? 0}
      </div>
      <div className="cert-dashboard-kpi-foot">{foot}</div>
    </div>
  );
}

function QualityCard({ label, value }: { label: string; value: number }) {
  return (
    <div className="cert-dashboard-resource">
      <div className="cert-dashboard-resource-title">{label}</div>
      <Progress type="dashboard" percent={value} size={110} />
    </div>
  );
}

/**
 * AI 同步学统计总览页 —— 《AI同步学统计功能设计方案》v1.1 §6.2
 * 隐藏路由：/training/exam-lists/:examId/review-sync-stats（考试管理列表页行内按钮进入）
 */
import { useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Col,
  DatePicker,
  Empty,
  Input,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import { ArrowLeftOutlined, DownloadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import ReactECharts from 'echarts-for-react';
import { useQuery } from '@tanstack/react-query';
import dayjs, { Dayjs } from 'dayjs';
import { pageExams } from '../../../api/training';
import { useAuthStore } from '../../../stores/authStore';
import {
  downloadBlob,
  exportStudents,
  fetchDailyTrend,
  fetchKnowledgeMastery,
  fetchStatsOverview,
  fetchStudents,
  fetchWrongTrend,
  type StudentRow,
} from './api';

const RANGE_OPTIONS = [
  { value: 7, label: '近 7 天' },
  { value: 14, label: '近 14 天' },
  { value: 30, label: '近 30 天' },
];

const STAGE_NAMES: Record<number, string> = { 1: '地毯式扫荡', 2: '定向爆破', 3: '仿真模考' };
const STATUS_TAGS: Record<string, { color: string; text: string }> = {
  active: { color: 'processing', text: '进行中' },
  completed: { color: 'success', text: '已完成' },
  abandoned: { color: 'default', text: '已放弃' },
};

/** 与 main.tsx 约定一致的路由跳转（pushState + popstate 事件） */
function navigateTo(path: string) {
  window.history.pushState({}, '', path);
  window.dispatchEvent(new PopStateEvent('popstate'));
}

type StatsOverviewPageProps = {
  examId: number;
  /** 嵌入跟班「同步学」Tab：隐藏返回/切计划，紧凑布局 */
  embedded?: boolean;
  /** false 时不发请求（如 Tab 未激活） */
  enabled?: boolean;
  /** 嵌入时展示的计划标题（可省） */
  examTitle?: string;
};

export function StatsOverviewPage({
  examId,
  embedded = false,
  enabled = true,
  examTitle,
}: StatsOverviewPageProps) {
  const hasPerm = useAuthStore((s) => s.hasPermission);
  const canExport = hasPerm('exam:review-sync:stats:export');
  const canView = hasPerm('exam:review-sync:stats:view');
  const active = enabled && examId > 0 && canView;

  const [rangeDays, setRangeDays] = useState(14);
  const [customRange, setCustomRange] = useState<[Dayjs, Dayjs] | null>(null);
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [exporting, setExporting] = useState(false);

  const { from, to } = useMemo(() => {
    if (customRange) {
      return {
        from: customRange[0].format('YYYY-MM-DD'),
        to: customRange[1].format('YYYY-MM-DD'),
      };
    }
    return {
      from: dayjs().subtract(rangeDays - 1, 'day').format('YYYY-MM-DD'),
      to: dayjs().format('YYYY-MM-DD'),
    };
  }, [rangeDays, customRange]);

  // 考试计划下拉（独立页用；嵌入模式不拉全量计划）
  const examsQuery = useQuery({
    queryKey: ['review-sync-stats-exam-options'],
    queryFn: () => pageExams({ page: 1, pageSize: 200 }),
    enabled: !embedded && active,
  });
  const examOptions = (examsQuery.data?.items ?? []).map((e) => ({
    value: e.id!,
    label: e.title ?? `计划#${e.id}`,
  }));
  const currentExamTitle =
    examTitle
    || examOptions.find((o) => o.value === examId)?.label
    || `计划#${examId}`;

  const query = { from, to, examId };
  const overviewQuery = useQuery({
    queryKey: ['review-sync-stats', 'overview', query],
    queryFn: () => fetchStatsOverview(query),
    enabled: active,
  });
  const trendQuery = useQuery({
    queryKey: ['review-sync-stats', 'daily-trend', query],
    queryFn: () => fetchDailyTrend(query),
    enabled: active,
  });
  const wrongQuery = useQuery({
    queryKey: ['review-sync-stats', 'wrong-trend', query],
    queryFn: () => fetchWrongTrend(query),
    enabled: active,
  });
  const knowledgeQuery = useQuery({
    queryKey: ['review-sync-stats', 'knowledge', examId],
    queryFn: () => fetchKnowledgeMastery(examId),
    enabled: active,
  });
  const studentsQuery = useQuery({
    queryKey: ['review-sync-stats', 'students', examId, keyword, page, pageSize],
    queryFn: () => fetchStudents({ page, pageSize, examId, keyword: keyword || undefined }),
    enabled: active,
  });

  const overview = overviewQuery.data;
  const trendSeries = trendQuery.data?.series ?? [];
  const wrongTrend = wrongQuery.data;

  // ------------------------------------------------------------------
  // 图表 option
  // ------------------------------------------------------------------

  const trendOption = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['作答数', '活跃学员', '正确率'], top: 0, left: 'center' },
    grid: { left: 48, right: 52, top: 44, bottom: 28 },
    xAxis: { type: 'category', data: trendSeries.map((p) => p.day.slice(5)) },
    yAxis: [
      { type: 'value' },
      { type: 'value', max: 100, min: 0, axisLabel: { formatter: '{value}%' } },
    ],
    series: [
      { name: '作答数', type: 'bar', barMaxWidth: 36, data: trendSeries.map((p) => p.answers), itemStyle: { color: '#73a9fa' } },
      { name: '活跃学员', type: 'line', data: trendSeries.map((p) => p.activeUsers), itemStyle: { color: '#50b66f' } },
      {
        name: '正确率', type: 'line', yAxisIndex: 1,
        data: trendSeries.map((p) => p.accuracy), itemStyle: { color: '#e7916c' },
      },
    ],
  };

  const wrongDist = wrongTrend?.distribution;
  const wrongDonutOption = {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, left: 'center', itemWidth: 12, itemHeight: 12, textStyle: { fontSize: 12 } },
    series: [{
      type: 'pie',
      radius: ['38%', '60%'],
      center: ['50%', '42%'],
      data: [
        { name: '红牌', value: wrongDist?.red ?? 0, itemStyle: { color: '#e55744' } },
        { name: '黄牌', value: wrongDist?.yellow ?? 0, itemStyle: { color: '#e7916c' } },
        { name: '已掌握', value: wrongDist?.green ?? 0, itemStyle: { color: '#50b66f' } },
        { name: '加星(顽固)', value: wrongDist?.star ?? 0, itemStyle: { color: '#b03a2e' } },
      ],
    }],
  };

  const newByDay = new Map((wrongTrend?.dailyNew ?? []).map((p) => [p.day, p.cnt]));
  const removedByDay = new Map((wrongTrend?.dailyRemoved ?? []).map((p) => [p.day, p.cnt]));
  const wrongDays = Array.from(new Set([...newByDay.keys(), ...removedByDay.keys()])).sort();
  const wrongDailyOption = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['新增错题', '消灭错题'], top: 0, left: 'center' },
    grid: { left: 40, right: 16, top: 36, bottom: 24 },
    xAxis: { type: 'category', data: wrongDays.map((d) => d.slice(5)) },
    yAxis: { type: 'value' },
    series: [
      { name: '新增错题', type: 'bar', stack: 'w', barMaxWidth: 24, data: wrongDays.map((d) => newByDay.get(d) ?? 0), itemStyle: { color: '#e55744' } },
      { name: '消灭错题', type: 'bar', stack: 'w', barMaxWidth: 24, data: wrongDays.map((d) => -(removedByDay.get(d) ?? 0)), itemStyle: { color: '#50b66f' } },
    ],
  };

  const l1List = knowledgeQuery.data?.l1 ?? [];
  const masteryOption = {
    tooltip: { trigger: 'axis' },
    grid: { left: 140, right: 40, top: 20, bottom: 28 },
    xAxis: { type: 'value', max: 100, name: '正确率%' },
    yAxis: { type: 'category', data: l1List.map((k) => k.title).reverse() },
    series: [{
      type: 'bar',
      data: l1List.map((k) => ({
        value: k.accuracy,
        itemStyle: { color: k.accuracy < 60 ? '#e55744' : k.accuracy < 80 ? '#e7916c' : '#50b66f' },
      })).reverse(),
      label: { show: true, position: 'right' as const, formatter: '{c}%' },
    }],
  };

  // ------------------------------------------------------------------
  // 学员明细表
  // ------------------------------------------------------------------

  const columns: ColumnsType<StudentRow> = [
    { title: '学员', dataIndex: 'realname', width: 100 },
    { title: '手机号', dataIndex: 'phone', width: 120, render: (v) => v ?? '-' },
    { title: '考试计划', dataIndex: 'examTitle', ellipsis: true },
    {
      title: '阶段/进度', key: 'stage', width: 170,
      render: (_, r) => (
        <span>
          <Tag color="blue">{STAGE_NAMES[r.stage] ?? `阶段${r.stage}`}</Tag>
          第{r.dayIndex}/{r.totalDays}天
        </span>
      ),
    },
    { title: '累计练题', dataIndex: 'answers', width: 90, sorter: (a, b) => a.answers - b.answers },
    {
      title: '正确率', dataIndex: 'accuracy', width: 90,
      render: (v: number) => (
        <span style={{ color: v < 60 ? '#e55744' : v < 80 ? '#e7916c' : '#50b66f' }}>{v}%</span>
      ),
    },
    {
      title: '错题(红/黄/加星)', key: 'wrong', width: 140,
      render: (_, r) => (
        <span>
          <Tag color="#e55744">红{r.redCnt}</Tag>
          <Tag color="#e7916c">黄{r.yellowCnt}</Tag>
          {r.starCnt > 0 && <Tag color="#b03a2e">★{r.starCnt}</Tag>}
        </span>
      ),
    },
    { title: '最近活跃', dataIndex: 'lastActiveDate', width: 110, render: (v) => v ?? '-' },
    {
      title: '状态', dataIndex: 'status', width: 90,
      render: (v: string) => {
        const tag = STATUS_TAGS[v] ?? { color: 'default', text: v };
        return <Tag color={tag.color}>{tag.text}</Tag>;
      },
    },
    {
      title: '操作', key: 'act', width: 80, fixed: 'right',
      render: (_, r) => (
        <Button
          type="link" size="small"
          onClick={() => navigateTo(`/training/exam-lists/${r.examId}/review-sync-stats/student/${r.planId}`)}
        >详情</Button>
      ),
    },
  ];

  const onExport = async () => {
    setExporting(true);
    try {
      const blob = await exportStudents({ examId, keyword: keyword || undefined });
      downloadBlob(blob, `AI同步学学员明细_${dayjs().format('YYYYMMDD_HHmmss')}.xlsx`);
      message.success('导出成功');
    } catch (e) {
      message.error(e instanceof Error ? e.message : '导出失败');
    } finally {
      setExporting(false);
    }
  };

  // ------------------------------------------------------------------
  // 渲染
  // ------------------------------------------------------------------

  if (!canView) {
    return (
      <Alert
        type="warning"
        showIcon
        message="无同步学统计查看权限"
        description="需要权限码 exam:review-sync:stats:view"
      />
    );
  }

  return (
    <div style={{ padding: embedded ? 0 : 16 }}>
      <Space style={{ marginBottom: 16 }} wrap>
        {!embedded && (
          <>
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigateTo('/training/exam-list')}>
              返回考试管理
            </Button>
            <Typography.Text strong style={{ fontSize: 16 }}>AI 同步学统计</Typography.Text>
            <span>当前计划：</span>
            <Select
              style={{ minWidth: 280 }}
              value={examId}
              options={examOptions}
              loading={examsQuery.isLoading}
              showSearch
              optionFilterProp="label"
              onChange={(v) => navigateTo(`/training/exam-lists/${v}/review-sync-stats`)}
            />
          </>
        )}
        {embedded && (
          <Typography.Text strong style={{ fontSize: 15 }}>
            同步学学情 · {currentExamTitle}
          </Typography.Text>
        )}
        <Select
          style={{ width: 120 }}
          value={customRange ? undefined : rangeDays}
          options={RANGE_OPTIONS}
          placeholder="自定义"
          onChange={(v) => { setRangeDays(v); setCustomRange(null); setPage(1); }}
        />
        <DatePicker.RangePicker
          value={customRange}
          onChange={(v) => {
            setCustomRange(v && v[0] && v[1] ? [v[0], v[1]] : null);
            setPage(1);
          }}
        />
      </Space>

      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={4}><Card><Statistic title="计划学员" value={overview?.studentCount ?? 0} suffix="人" /></Card></Col>
        <Col span={4}><Card><Statistic title="今日活跃" value={overview?.todayActiveUsers ?? 0} suffix="人" /></Card></Col>
        <Col span={4}><Card><Statistic title="今日作答" value={overview?.todayAnswers ?? 0} suffix="题" /></Card></Col>
        <Col span={4}><Card><Statistic title="累计作答" value={overview?.totalAnswers ?? 0} suffix="题" /></Card></Col>
        <Col span={4}>
          <Card><Statistic title="整体正确率" value={overview?.overallAccuracy ?? 0} suffix="%" /></Card>
        </Col>
        <Col span={4}>
          <Card>
            <Statistic
              title="在册错题(红/黄/加星)"
              value={overview?.wrong.active ?? 0}
              suffix={`红${overview?.wrong.red ?? 0} 黄${overview?.wrong.yellow ?? 0} ★${overview?.wrong.star ?? 0}`}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={14}>
          <Card title="每日使用趋势" size="small">
            {trendSeries.length === 0
              ? <Empty description="所选范围内暂无 AI 同步练习数据" />
              : <ReactECharts option={trendOption} style={{ height: 300 }} />}
          </Card>
        </Col>
        <Col span={10}>
          <Card title="错题牌级分布" size="small" style={{ marginBottom: 8 }}>
            {(wrongDist?.active ?? 0) + (wrongDist?.green ?? 0) === 0
              ? <Empty description="暂无错题" />
              : <ReactECharts option={wrongDonutOption} style={{ height: 190 }} />}
          </Card>
          <Card title="每日新增/消灭错题" size="small">
            {wrongDays.length === 0
              ? <Empty description="暂无错题变动" />
              : <ReactECharts option={wrongDailyOption} style={{ height: 190 }} />}
          </Card>
        </Col>
      </Row>

      <Card title="考核项目掌握度（L1）" size="small" style={{ marginBottom: 16 }}>
        {l1List.length === 0
          ? <Empty description="暂无作答数据" />
          : <ReactECharts option={masteryOption} style={{ height: Math.max(220, l1List.length * 40) }} />}
      </Card>

      <Card
        title="学员明细"
        size="small"
        extra={(
          <Space>
            <Input.Search
              placeholder="学员姓名/手机号"
              allowClear
              style={{ width: 200 }}
              onSearch={(v) => { setKeyword(v.trim()); setPage(1); }}
            />
            {canExport && (
              <Button icon={<DownloadOutlined />} loading={exporting} onClick={onExport}>
                导出 Excel
              </Button>
            )}
          </Space>
        )}
      >
        <Table<StudentRow>
          rowKey="planId"
          size="small"
          loading={studentsQuery.isLoading}
          columns={columns}
          dataSource={studentsQuery.data?.items ?? []}
          scroll={{ x: 1200 }}
          pagination={{
            current: page,
            pageSize,
            total: studentsQuery.data?.total ?? 0,
            showSizeChanger: true,
            showTotal: (t) => `共 ${t} 条`,
            onChange: (p, ps) => {
              if (typeof ps === 'number' && ps > 0 && ps !== pageSize) {
                setPageSize(ps);
                setPage(1);
              } else {
                setPage(p);
              }
            },
          }}
        />
      </Card>
    </div>
  );
}

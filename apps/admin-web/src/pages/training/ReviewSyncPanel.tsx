import ReactECharts from 'echarts-for-react';
import { useQuery } from '@tanstack/react-query';
import { Alert, Button, Card, Space, Spin, Tag, Tooltip, Typography } from 'antd';
import { LineChartOutlined } from '@ant-design/icons';
import { getReviewSyncPanel } from '../../api/training';
import type { ReviewSyncPanel } from '../../types/training';
import { useAuthStore } from '../../stores/authStore';
import { useNavStore } from '../../stores/navStore';
import { usePaneActive } from '../../layouts/TabKeepAlive';

/** 红绿灯阈值（方案 §4.3，集中在前端常量便于调） */
const ACTIVE_RATE_RED = 40; // 7日活跃率 <40% 标红
const PASS_RATE_RED = 40; // 模考及格率 <40% 标红
const PASS_RATE_GREEN = 60; // ≥60% 标绿
const MOCK_MIN_SAMPLE = 10; // 模考完成 <10 套显示"--"（样本不足）

const POLL_INTERVAL_MS = 60_000;

/** 明细页=考试计划列表（行内「AI同步学统计」入口）；SPA 内容区按 selectedKey 渲染，不走 URL */
const DETAIL_MENU_KEY = 'kao_shi_ji_hua';

function formatWan(value?: number | null) {
  if (value == null) return '--';
  return value >= 10000 ? `${(value / 10000).toFixed(1)}万` : String(value);
}

function formatRate(value?: number | null) {
  return value == null ? '--' : `${Number(value).toFixed(1)}%`;
}

function rateTag(value: number | null, redBelow?: number, redAbove?: number, greenAtOrAbove?: number) {
  if (value == null) return null;
  if (redAbove != null && value > redAbove) return <Tag color="red">偏高</Tag>;
  if (redBelow != null && value < redBelow) return <Tag color="red">偏低</Tag>;
  if (greenAtOrAbove != null && value >= greenAtOrAbove) return <Tag color="green">正常</Tag>;
  return null;
}

function Kpi({ label, value, foot, tag }: { label: string; value: string; foot?: React.ReactNode; tag?: React.ReactNode }) {
  return (
    <div className="review-sync-kpi">
      <div className="review-sync-kpi-label">{label}</div>
      <div className="review-sync-kpi-value">
        {value}
        {tag ? <span style={{ marginLeft: 8 }}>{tag}</span> : null}
      </div>
      {foot ? <div className="review-sync-kpi-foot">{foot}</div> : null}
    </div>
  );
}

function GroupTitle({ children }: { children: React.ReactNode }) {
  return <div className="review-sync-group-title">{children}</div>;
}

/** 运营数据面板「同步练习」分区（方案 v0.1 §4）：一次拿全，60s 轮询，局部降级 */
export function ReviewSyncPanel() {
  const hasPerm = useAuthStore((s) => s.hasPermission);
  const canViewStats = hasPerm('exam:review-sync:stats:view');
  const setSelectedKey = useNavStore((s) => s.setSelectedKey);
  const paneActive = usePaneActive(); // 标签页不可见时暂停 60s 轮询（§3.6）

  const panelQuery = useQuery<ReviewSyncPanel>({
    queryKey: ['training-dashboard-review-sync-panel'],
    queryFn: () => getReviewSyncPanel(30),
    refetchInterval: paneActive ? POLL_INTERVAL_MS : false,
    refetchIntervalInBackground: false,
    refetchOnWindowFocus: true,
    staleTime: 0,
  });

  if (panelQuery.isLoading && !panelQuery.data) {
    return (
      <Card size="small" title="同步练习（AI 同步学）">
        <Spin size="small" style={{ display: 'block', margin: 24 }} />
      </Card>
    );
  }

  if (panelQuery.isError || !panelQuery.data) {
    return (
      <Alert
        type="warning"
        showIcon
        message="同步练习统计加载失败"
        action={<Button size="small" onClick={() => void panelQuery.refetch()}>重试</Button>}
      />
    );
  }

  const { scale, effort, effect, trend30d, wrongTrend30d, weakTop5 } = panelQuery.data;
  const mockSampleEnough = effect.mockDone >= MOCK_MIN_SAMPLE;

  const trendOption = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['作答数', '活跃学员', '正确率'], top: 0 },
    grid: { left: 48, right: 48, top: 32, bottom: 24 },
    xAxis: { type: 'category', data: trend30d.map((p) => p.date.slice(5)) },
    yAxis: [
      { type: 'value', name: '题数/人数' },
      { type: 'value', name: '正确率%', max: 100, min: 0 },
    ],
    series: [
      { name: '作答数', type: 'bar', data: trend30d.map((p) => p.answers), itemStyle: { color: '#7fb3ff' } },
      { name: '活跃学员', type: 'line', smooth: true, data: trend30d.map((p) => p.activeUsers), itemStyle: { color: '#52c41a' } },
      { name: '正确率', type: 'line', yAxisIndex: 1, smooth: true, data: trend30d.map((p) => p.correctRate), itemStyle: { color: '#722ed1' } },
    ],
  };

  const wrongOption = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['新增错题', '消灭错题'], top: 0 },
    grid: { left: 48, right: 24, top: 32, bottom: 24 },
    xAxis: { type: 'category', data: wrongTrend30d.map((p) => p.date.slice(5)) },
    yAxis: { type: 'value' },
    series: [
      { name: '消灭错题', type: 'bar', stack: 'w', data: wrongTrend30d.map((p) => p.removedCnt), itemStyle: { color: '#52c41a' } },
      { name: '新增错题', type: 'bar', stack: 'w', data: wrongTrend30d.map((p) => p.newCnt), itemStyle: { color: '#ff4d4f' } },
    ],
  };

  const weakOption = {
    tooltip: { trigger: 'axis', valueFormatter: (v: unknown) => `${v}%` },
    grid: { left: 120, right: 48, top: 8, bottom: 24 },
    xAxis: { type: 'value', max: 100 },
    yAxis: { type: 'category', data: [...weakTop5].reverse().map((w) => w.title) },
    series: [{
      type: 'bar',
      data: [...weakTop5].reverse().map((w) => w.correctRate),
      itemStyle: { color: '#ff7875' },
      label: { show: true, position: 'right' as const, formatter: (p: { value?: number | null }) => (p.value == null ? '--' : `${p.value}%`) },
    }],
  };

  const detailLink = canViewStats ? (
    <Button type="link" size="small" icon={<LineChartOutlined />}
      onClick={() => setSelectedKey(DETAIL_MENU_KEY)}>
      查看明细
    </Button>
  ) : (
    <Tooltip title="需考试管理权限">
      <Button type="link" size="small" icon={<LineChartOutlined />} disabled>
        查看明细
      </Button>
    </Tooltip>
  );

  return (
    <Card
      size="small"
      title={
        <Space>
          同步练习（AI 同步学）
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>近30天口径</Typography.Text>
        </Space>
      }
      extra={detailLink}
    >
      <GroupTitle>规模</GroupTitle>
      <div className="review-sync-kpi-row">
        <Kpi label="计划学员" value={formatWan(scale.totalPlans)} foot={`已完成 ${scale.completed}`} />
        <Kpi label="进行中" value={formatWan(scale.active)} />
        <Kpi label="近7日新增计划" value={formatWan(scale.newPlans7d)} foot="含今天" />
        <Kpi label="近7日活跃" value={formatWan(scale.active7d)}
          tag={rateTag(scale.activeRate7d, ACTIVE_RATE_RED)}
          foot={`7日活跃率 ${formatRate(scale.activeRate7d)}`} />
        <Kpi label="累计中断" value={`${scale.interruptPlans} 人次`} />
      </div>

      <GroupTitle>投入</GroupTitle>
      <div className="review-sync-kpi-row">
        <Kpi label="累计作答" value={formatWan(effort.totalAnswers)} />
        <Kpi label="人均作答" value={effort.avgAnswersPerPlan == null ? '--' : `${effort.avgAnswersPerPlan} 题`} />
        <Kpi label="任务完成率" value={formatRate(effort.dayCompletionRate)} />
        <Kpi label="今日作答" value={formatWan(effort.todayAnswers)} foot={`活跃 ${effort.todayActive} 人`} />
      </div>

      <GroupTitle>效果</GroupTitle>
      <div className="review-sync-kpi-row">
        <Kpi label="整体正确率" value={formatRate(effect.correctRate)} />
        <Kpi label="累计消灭错题" value={formatWan(effect.removedWrongs)}
          foot={`在册 红${formatWan(effect.redWrongs)} / 黄${formatWan(effect.yellowWrongs)}`} />
        <Kpi label="模考完成" value={mockSampleEnough ? `${effect.mockDone} 套` : `${effect.mockDone} 套`}
          foot={`平均分 ${effect.mockAvgScore == null ? '--' : effect.mockAvgScore}`} />
        <Kpi
          label="模考及格率"
          value={mockSampleEnough ? formatRate(effect.mockPassRate) : '--'}
          tag={mockSampleEnough ? rateTag(effect.mockPassRate, PASS_RATE_RED, undefined, PASS_RATE_GREEN) : null}
          foot={mockSampleEnough ? undefined : '样本不足'}
        />
      </div>

      <div className="training-dashboard-chart-grid" style={{ marginTop: 12 }}>
        <Card size="small" title="近30天活跃与正确率">
          <ReactECharts option={trendOption} style={{ height: 280 }} />
        </Card>
        <Card size="small" title="错题新增 vs 消灭">
          <ReactECharts option={wrongOption} style={{ height: 280 }} />
        </Card>
      </div>

      <Card size="small" title="薄弱考核项目 Top5（L1，正确率最低，近30天作答≥50次参评）" style={{ marginTop: 12 }}>
        {weakTop5.length === 0 ? (
          <Typography.Text type="secondary">暂无满足参评门槛的数据</Typography.Text>
        ) : (
          <ReactECharts option={weakOption} style={{ height: Math.max(160, weakTop5.length * 40) }} />
        )}
      </Card>
    </Card>
  );
}

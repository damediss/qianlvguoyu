/**
 * AI 同步学学员详情页 —— 《AI同步学统计功能设计方案》v1.1 §6.3
 * 隐藏路由：/training/exam-lists/:examId/review-sync-stats/student/:planId
 */
import { useMemo, type CSSProperties } from 'react';
import {
  Button,
  Card,
  Col,
  Empty,
  Progress,
  Row,
  Space,
  Statistic,
  Table,
  Tag,
  Typography,
} from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import ReactECharts from 'echarts-for-react';
import { useQuery } from '@tanstack/react-query';
import { fetchStudentDetail, type StudentMockPaper, type StudentWrongItem } from './api';

const STAGE_NAMES: Record<number, string> = { 1: '地毯式扫荡', 2: '定向爆破', 3: '仿真模考' };
const CARD_LEVELS: Record<string, { color: string; text: string }> = {
  red: { color: '#e55744', text: '红牌' },
  yellow: { color: '#e7916c', text: '黄牌' },
  green: { color: '#50b66f', text: '已掌握' },
};
const TYPE_TEXT: Record<string, string> = {
  single: '单选题', multi: '多选题', judge: '判断题', cal: '计算题', answer: '简答题',
};

/** 题干可能含 HTML 标签：表格折叠态显示纯文本摘要 */
function stripHtml(html: string | null | undefined): string {
  if (!html) {
    return '';
  }
  const div = document.createElement('div');
  div.innerHTML = html;
  return (div.textContent || div.innerText || '').trim();
}

/** 学员答案归一化为数组：多选后端存 JSON 数组字符串，单选/判断存选项内容原文 */
function parseUserAnswers(userAnswer: string | null | undefined): string[] {
  if (!userAnswer) {
    return [];
  }
  const trimmed = userAnswer.trim();
  if (trimmed.startsWith('[')) {
    try {
      const parsed = JSON.parse(trimmed);
      if (Array.isArray(parsed)) {
        return parsed.map(String);
      }
    } catch {
      // 非合法 JSON 时按原文处理
    }
  }
  return [trimmed];
}

/** 渲染可能含 HTML 的题干/选项（管理端内部数据，题库管理人员录入） */
function HtmlBlock({ html, style }: { html: string; style?: CSSProperties }) {
  return <div style={style} dangerouslySetInnerHTML={{ __html: html }} />;
}

function navigateTo(path: string) {
  window.history.pushState({}, '', path);
  window.dispatchEvent(new PopStateEvent('popstate'));
}

export function StudentDetailPage({ examId, planId }: { examId: number; planId: string }) {
  const detailQuery = useQuery({
    queryKey: ['review-sync-stats', 'student', planId],
    queryFn: () => fetchStudentDetail(planId),
  });
  const detail = detailQuery.data;
  const plan = detail?.plan;

  const daily = detail?.daily ?? [];
  const dailyOption = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['作答数', '正确率'], top: 0, left: 'center' },
    grid: { left: 44, right: 52, top: 44, bottom: 28 },
    xAxis: { type: 'category', data: daily.map((p) => p.day.slice(5)) },
    yAxis: [
      { type: 'value' },
      { type: 'value', max: 100, min: 0, axisLabel: { formatter: '{value}%' } },
    ],
    series: [
      { name: '作答数', type: 'bar', barMaxWidth: 36, data: daily.map((p) => p.answers), itemStyle: { color: '#73a9fa' } },
      {
        name: '正确率', type: 'line', yAxisIndex: 1,
        data: daily.map((p) => p.accuracy), itemStyle: { color: '#e7916c' },
      },
    ],
  };

  const dist = detail?.wrongDistribution;
  const donutOption = {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, left: 'center', itemWidth: 12, itemHeight: 12, textStyle: { fontSize: 12 } },
    series: [{
      type: 'pie',
      radius: ['38%', '60%'],
      center: ['50%', '42%'],
      data: [
        { name: '红牌', value: dist?.red ?? 0, itemStyle: { color: '#e55744' } },
        { name: '黄牌', value: dist?.yellow ?? 0, itemStyle: { color: '#e7916c' } },
        { name: '已掌握', value: dist?.green ?? 0, itemStyle: { color: '#50b66f' } },
        { name: '加星', value: dist?.star ?? 0, itemStyle: { color: '#b03a2e' } },
      ],
    }],
  };

  const l1List = detail?.knowledge.l1 ?? [];
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

  const wrongColumns: ColumnsType<StudentWrongItem> = [
    { title: '题号', dataIndex: 'questionId', width: 100 },
    {
      title: '题型', dataIndex: 'questionType', width: 90,
      render: (v: string | null) => (v ? (TYPE_TEXT[v] ?? v) : '-'),
    },
    {
      title: '题目', dataIndex: 'content', ellipsis: true,
      render: (v: string | null) => {
        const text = stripHtml(v);
        return text ? (
          <span>{text.length > 60 ? `${text.substring(0, 60)}…` : text}</span>
        ) : <Typography.Text type="secondary">题目详情加载失败</Typography.Text>;
      },
    },
    {
      title: '正确答案', dataIndex: 'correctAnswer', width: 140, ellipsis: true,
      render: (v: string | null) => (v ? <span style={{ color: '#1f7a38' }}>{stripHtml(v)}</span> : '-'),
    },
    {
      title: '学员最近答案', dataIndex: 'userAnswer', width: 140, ellipsis: true,
      render: (v: string | null, r) => {
        const answers = parseUserAnswers(v);
        if (answers.length === 0) {
          return '-';
        }
        const text = answers.map(stripHtml).join('、');
        return <span style={{ color: r.lastCorrect ? '#1f7a38' : '#b03a2e' }}>{text}</span>;
      },
    },
    {
      title: '牌级', dataIndex: 'cardLevel', width: 110,
      render: (v: string, r) => {
        const lv = CARD_LEVELS[v] ?? { color: 'default', text: v };
        return (
          <Tag color={lv.color}>
            {lv.text}{r.star === 1 ? ' ★' : ''}{r.removed === 1 ? '(已移出)' : ''}
          </Tag>
        );
      },
    },
    { title: '已错/已对', key: 'wc', width: 90, render: (_, r) => `${r.wrongCount}/${r.correctCount}` },
    { title: '最近答错', dataIndex: 'lastWrongTime', width: 160, render: (v) => v?.slice(0, 16).replace('T', ' ') ?? '-' },
  ];

  /** 展开行：完整题目 + 全部选项（标注正确答案与学员选择） */
  const wrongExpandedRender = (r: StudentWrongItem) => {
    const userAnswers = parseUserAnswers(r.userAnswer);
    return (
      <div style={{ padding: '8px 12px', background: '#fafafa', borderRadius: 8 }}>
        <div style={{ marginBottom: 8 }}>
          <Typography.Text strong>【{TYPE_TEXT[r.questionType ?? ''] ?? r.questionType ?? '题目'}】</Typography.Text>
          {r.content
            ? <HtmlBlock html={r.content} style={{ display: 'inline-block', marginLeft: 6 }} />
            : <Typography.Text type="secondary"> 题目详情加载失败（PHP 题库不可达）</Typography.Text>}
        </div>
        {r.options.length > 0 && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            {r.options.map((opt, i) => {
              const picked = userAnswers.includes(opt.content);
              return (
                <div
                  key={i}
                  style={{
                    padding: '6px 10px',
                    borderRadius: 6,
                    border: `1px solid ${opt.correct ? '#50b66f' : picked ? '#e55744' : '#e5e6eb'}`,
                    background: opt.correct ? '#eafaf0' : picked ? '#fdeceb' : '#fff',
                  }}
                >
                  {opt.correct && <span style={{ color: '#1f7a38', marginRight: 6 }}>✓ 正确答案</span>}
                  {picked && !opt.correct && <span style={{ color: '#b03a2e', marginRight: 6 }}>✗ 学员选择</span>}
                  {picked && opt.correct && <span style={{ color: '#1f7a38', marginRight: 6 }}>（学员选对）</span>}
                  <HtmlBlock html={opt.content} style={{ display: 'inline-block' }} />
                </div>
              );
            })}
          </div>
        )}
      </div>
    );
  };

  const mockColumns: ColumnsType<StudentMockPaper> = [
    { title: '第几套', dataIndex: 'paperNo', width: 80 },
    {
      title: '得分', key: 'score', width: 120,
      render: (_, r) => `${r.score ?? '-'} / ${r.totalScore ?? '-'}`,
    },
    { title: '答对', dataIndex: 'correctCount', width: 80, render: (v, r) => `${v ?? '-'}/${r.questionCount ?? '-'}` },
    {
      title: '是否及格', key: 'passed', width: 90,
      render: (_, r) => (r.passed ? <Tag color="success">及格</Tag> : <Tag color="error">未及格</Tag>),
    },
    { title: '交卷时间', dataIndex: 'createdAt', render: (v) => v?.slice(0, 16).replace('T', ' ') ?? '-' },
  ];

  const progressPercent = useMemo(() => {
    if (!plan || !plan.targetQuestions) {
      return 0;
    }
    const answered = (detail?.daily ?? []).reduce((sum, p) => sum + p.answers, 0);
    return Math.min(100, Math.round((answered * 100) / plan.targetQuestions));
  }, [plan, detail]);

  return (
    <div style={{ padding: 16 }}>
      <Space style={{ marginBottom: 16 }} wrap>
        <Button
          icon={<ArrowLeftOutlined />}
          onClick={() => navigateTo(`/training/exam-lists/${examId}/review-sync-stats`)}
        >
          返回统计
        </Button>
        <Typography.Text strong style={{ fontSize: 16 }}>
          {plan?.realname ?? '学员详情'}
        </Typography.Text>
        {plan?.phone && <Typography.Text type="secondary">{plan.phone}</Typography.Text>}
        <Typography.Text type="secondary">|</Typography.Text>
        <Typography.Text>{plan?.examTitle ?? `计划#${examId}`}</Typography.Text>
        {plan && (
          <Tag color="blue">{STAGE_NAMES[plan.stage] ?? `阶段${plan.stage}`} · 第{plan.dayIndex}/{plan.totalDays}天</Tag>
        )}
      </Space>

      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={8}>
          <Card size="small" title="必做池进度">
            <Progress percent={progressPercent} status="active" />
            <Typography.Text type="secondary">
              必做 {plan?.targetQuestions ?? 0} 题；考试日期 {plan?.examDate ?? '未定'}
            </Typography.Text>
          </Card>
        </Col>
        <Col span={4}><Card size="small"><Statistic title="红牌" value={dist?.red ?? 0} valueStyle={{ color: '#e55744' }} /></Card></Col>
        <Col span={4}><Card size="small"><Statistic title="黄牌" value={dist?.yellow ?? 0} valueStyle={{ color: '#e7916c' }} /></Card></Col>
        <Col span={4}><Card size="small"><Statistic title="已掌握" value={dist?.green ?? 0} valueStyle={{ color: '#50b66f' }} /></Card></Col>
        <Col span={4}><Card size="small"><Statistic title="加星顽固" value={dist?.star ?? 0} valueStyle={{ color: '#b03a2e' }} /></Card></Col>
      </Row>

      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={14}>
          <Card title="个人每日趋势" size="small">
            {daily.length === 0
              ? <Empty description="暂无作答数据" />
              : <ReactECharts option={dailyOption} style={{ height: 280 }} />}
          </Card>
        </Col>
        <Col span={10}>
          <Card title="错题牌级" size="small">
            {(dist?.active ?? 0) + (dist?.green ?? 0) === 0
              ? <Empty description="暂无错题" />
              : <ReactECharts option={donutOption} style={{ height: 280 }} />}
          </Card>
        </Col>
      </Row>

      <Card title="知识点掌握度（L1 考核项目）" size="small" style={{ marginBottom: 16 }}>
        {l1List.length === 0
          ? <Empty description="暂无作答数据" />
          : <ReactECharts option={masteryOption} style={{ height: Math.max(200, l1List.length * 40) }} />}
      </Card>

      <Card title="错题清单（点击行展开完整题目）" size="small" style={{ marginBottom: 16 }}>
        <Table<StudentWrongItem>
          rowKey="questionId"
          size="small"
          columns={wrongColumns}
          dataSource={detail?.wrongList ?? []}
          expandable={{ expandedRowRender: wrongExpandedRender, rowExpandable: (r) => r.content != null }}
          pagination={{ pageSize: 20, showTotal: (t) => `共 ${t} 条` }}
        />
      </Card>

      <Card title="模考成绩" size="small">
        <Table<StudentMockPaper>
          rowKey="paperNo"
          size="small"
          columns={mockColumns}
          dataSource={detail?.mockPapers ?? []}
          pagination={false}
          locale={{ emptyText: '尚未参加模考' }}
        />
      </Card>
    </div>
  );
}

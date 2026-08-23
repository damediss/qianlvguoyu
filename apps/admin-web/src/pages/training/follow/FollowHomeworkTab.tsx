import { useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Descriptions,
  Drawer,
  Empty,
  Progress,
  Space,
  Statistic,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import { DownloadOutlined, ReloadOutlined, TeamOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../../api/training';

type Props = { classId: number };

const STATUS_COLOR: Record<string, string> = {
  draft: 'default',
  published: 'processing',
  closed: 'default',
  pending: 'gold',
  submitted: 'blue',
  graded: 'green',
};

function formatDuration(sec?: number | null) {
  if (sec == null || sec < 0) return '—';
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  if (m <= 0) return `${s}秒`;
  return `${m}分${String(s).padStart(2, '0')}秒`;
}

function stripHtml(raw?: string | null) {
  if (!raw) return '';
  return String(raw)
    .replace(/<br\s*\/?>/gi, ' ')
    .replace(/<\/p\s*>/gi, ' ')
    .replace(/<[^>]+>/g, '')
    .replace(/&nbsp;/gi, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function formatAnswerJson(raw?: string | null) {
  if (!raw) return '—';
  try {
    const v = JSON.parse(raw);
    if (Array.isArray(v)) return v.join('、');
    if (typeof v === 'boolean') return v ? '正确' : '错误';
    if (v === 'true') return '正确';
    if (v === 'false') return '错误';
    return String(v);
  } catch {
    return raw;
  }
}

export function FollowHomeworkTab({ classId }: Props) {
  const queryClient = useQueryClient();
  const [taskId, setTaskId] = useState<string | number | null>(null);
  const [answerItem, setAnswerItem] = useState<trainApi.FollowTaskItem | null>(null);
  const [exporting, setExporting] = useState(false);

  const { data: metrics, isLoading: metricsLoading } = useQuery({
    queryKey: ['homework-metrics', classId],
    queryFn: () => trainApi.getHomeworkMetrics(classId),
    enabled: classId > 0,
  });

  const { data: tasks, isLoading, refetch } = useQuery({
    queryKey: ['follow-tasks', classId, 'assignment'],
    queryFn: () => trainApi.listFollowTasks(classId, 'assignment'),
    enabled: classId > 0,
  });

  const { data: items, isLoading: itemsLoading } = useQuery({
    queryKey: ['follow-task-items', taskId],
    queryFn: () => trainApi.listFollowTaskItems(taskId!),
    enabled: taskId != null,
  });

  const { data: stats, isLoading: statsLoading } = useQuery({
    queryKey: ['follow-task-stats', taskId],
    queryFn: () => trainApi.getFollowTaskStats(taskId!),
    enabled: taskId != null,
  });

  const { data: answers, isLoading: answersLoading } = useQuery({
    queryKey: ['follow-task-answers', taskId, answerItem?.id],
    queryFn: () => trainApi.getFollowTaskItemAnswers(taskId!, answerItem!.id),
    enabled: taskId != null && answerItem != null,
  });

  const syncMu = useMutation({
    mutationFn: (id: string | number) => trainApi.syncFollowTaskRoster(id),
    onSuccess: (r, id) => {
      message.success(`已补发 ${r.added} 人`);
      void queryClient.invalidateQueries({ queryKey: ['follow-task-items', id] });
      void queryClient.invalidateQueries({ queryKey: ['follow-tasks', classId] });
      void queryClient.invalidateQueries({ queryKey: ['homework-metrics', classId] });
      void queryClient.invalidateQueries({ queryKey: ['follow-task-stats', id] });
    },
    onError: (e: Error) => message.error(e.message || '补发失败'),
  });

  const closeMu = useMutation({
    mutationFn: (id: string | number) => trainApi.closeFollowTask(id),
    onSuccess: () => {
      message.success('已关闭');
      void queryClient.invalidateQueries({ queryKey: ['follow-tasks', classId] });
      void queryClient.invalidateQueries({ queryKey: ['homework-metrics', classId] });
    },
    onError: (e: Error) => message.error(e.message || '关闭失败'),
  });

  const reopenMu = useMutation({
    mutationFn: (id: string | number) => trainApi.reopenFollowTask(id),
    onSuccess: () => {
      message.success('已重新打开');
      void queryClient.invalidateQueries({ queryKey: ['follow-tasks', classId] });
      void queryClient.invalidateQueries({ queryKey: ['homework-metrics', classId] });
    },
    onError: (e: Error) => message.error(e.message || '重新打开失败'),
  });

  const weakQuestions = useMemo(() => {
    const list = stats?.questionStats ?? [];
    return [...list]
      .filter((q) => q.accuracyRate != null)
      .sort((a, b) => (a.accuracyRate ?? 100) - (b.accuracyRate ?? 100))
      .slice(0, 5);
  }, [stats]);

  const taskColumns: ColumnsType<trainApi.FollowTask> = [
    {
      title: '类型',
      width: 90,
      render: (_, r) => r.taskTypeName || r.taskType,
    },
    { title: '标题', dataIndex: 'title', ellipsis: true },
    {
      title: '挂载',
      width: 120,
      render: (_, r) =>
        r.scheduleId != null ? `课次#${r.scheduleId}` : r.bizDate || '—',
    },
    {
      title: '状态',
      width: 90,
      render: (_, r) => (
        <Tag color={STATUS_COLOR[r.status] || 'default'}>{r.statusName || r.status}</Tag>
      ),
    },
    {
      title: '完成率',
      width: 90,
      render: (_, r) =>
        r.completionRate != null ? `${r.completionRate}%` : r.status === 'draft' ? '—' : '0%',
    },
    {
      title: '进度',
      width: 110,
      render: (_, r) =>
        r.totalCount != null ? `${r.submittedCount ?? 0}/${r.totalCount}` : '—',
    },
    {
      title: '操作',
      width: 220,
      render: (_, r) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            onClick={() => {
              setTaskId(r.id);
              setAnswerItem(null);
            }}
          >
            明细
          </Button>
          {r.status === 'published' ? (
            <>
              <Button
                type="link"
                size="small"
                loading={syncMu.isPending}
                onClick={() => syncMu.mutate(r.id)}
              >
                补发名单
              </Button>
              <Button
                type="link"
                size="small"
                danger
                loading={closeMu.isPending}
                onClick={() => closeMu.mutate(r.id)}
              >
                关闭
              </Button>
            </>
          ) : null}
          {r.status === 'closed' ? (
            <Button
              type="link"
              size="small"
              loading={reopenMu.isPending}
              onClick={() => reopenMu.mutate(r.id)}
            >
              重新打开
            </Button>
          ) : null}
        </Space>
      ),
    },
  ];

  const itemColumns: ColumnsType<trainApi.FollowTaskItem> = [
    { title: '学员', dataIndex: 'userName', width: 100 },
    {
      title: '状态',
      width: 80,
      render: (_, r) => (
        <Tag color={STATUS_COLOR[r.status] || 'default'}>{r.statusName || r.status}</Tag>
      ),
    },
    {
      title: '得分',
      width: 70,
      render: (_, r) => (r.score != null ? r.score : '—'),
    },
    {
      title: '正确率',
      width: 90,
      render: (_, r) =>
        r.accuracyRate != null ? `${r.accuracyRate}%` : r.status === 'pending' ? '—' : '—',
    },
    {
      title: '时长',
      width: 90,
      render: (_, r) => formatDuration(r.durationSeconds),
    },
    {
      title: '达标',
      width: 70,
      render: (_, r) => (r.passed == null ? '—' : r.passed ? '是' : '否'),
    },
    {
      title: '提交时间',
      dataIndex: 'submittedAt',
      width: 150,
      render: (v?: string) => (v ? String(v).replace('T', ' ').slice(0, 16) : '—'),
    },
    {
      title: '操作',
      width: 80,
      render: (_, r) =>
        r.status !== 'pending' ? (
          <Button type="link" size="small" onClick={() => setAnswerItem(r)}>
            答卷
          </Button>
        ) : (
          '—'
        ),
    },
  ];

  const qStatColumns: ColumnsType<trainApi.FollowTaskQuestionStat> = [
    { title: '题号', dataIndex: 'sortNumber', width: 60 },
    {
      title: '题干',
      ellipsis: true,
      render: (_, r) => stripHtml(r.stemPreview) || '—',
    },
    {
      title: '正确率',
      width: 140,
      render: (_, r) =>
        r.accuracyRate == null ? (
          '—'
        ) : (
          <Space size={8}>
            <Progress
              percent={Number(r.accuracyRate)}
              size="small"
              style={{ width: 72, margin: 0 }}
              status={Number(r.accuracyRate) < 60 ? 'exception' : 'normal'}
              showInfo={false}
            />
            <span>{r.accuracyRate}%</span>
          </Space>
        ),
    },
    {
      title: '作答/正确',
      width: 100,
      render: (_, r) => `${r.correctCount ?? 0}/${r.answeredCount ?? 0}`,
    },
  ];

  return (
    <div>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 12 }}
        message="课堂作业 / 随堂练"
        description="老师在 App 理论老师台布置；本页查看进度、正确率、答卷与导出。与提升「考前练习」、班级刷题分通道。"
      />
      <Space size="large" style={{ marginBottom: 16 }} wrap>
        <Statistic
          title="已发布任务"
          value={metrics?.taskCount ?? 0}
          loading={metricsLoading}
          prefix={<TeamOutlined />}
        />
        <Statistic title="应交人次" value={metrics?.totalItems ?? 0} loading={metricsLoading} />
        <Statistic title="已交人次" value={metrics?.gradedItems ?? 0} loading={metricsLoading} />
        <Statistic
          title="完成率"
          value={metrics?.completionRate ?? 0}
          suffix="%"
          loading={metricsLoading}
        />
        <Statistic
          title="平均正确率"
          value={metrics?.avgAccuracyRate ?? '—'}
          suffix={metrics?.avgAccuracyRate != null ? '%' : undefined}
          loading={metricsLoading}
        />
        <Button icon={<ReloadOutlined />} onClick={() => void refetch()}>
          刷新
        </Button>
      </Space>

      {(metrics?.warnings?.length ?? 0) > 0 ? (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 12 }}
          message="学员预警"
          description={
            <ul style={{ margin: 0, paddingLeft: 18 }}>
              {(metrics?.warnings ?? []).slice(0, 8).map((w, i) => (
                <li key={`${w.peopleId}-${w.type}-${i}`}>
                  {w.userName || w.peopleId}：{w.message}
                </li>
              ))}
            </ul>
          }
        />
      ) : null}

      <Table
        rowKey={(r) => String(r.id)}
        size="small"
        loading={isLoading}
        columns={taskColumns}
        dataSource={tasks ?? []}
        pagination={{ pageSize: 20, showSizeChanger: false, showTotal: (t) => `共 ${t} 条` }}
        locale={{ emptyText: <Empty description="暂无课堂作业；请老师在 App 布置" /> }}
      />

      <Drawer
        title="任务提交明细"
        width={760}
        open={taskId != null}
        onClose={() => {
          setTaskId(null);
          setAnswerItem(null);
        }}
        extra={
          taskId != null ? (
            <Button
              icon={<DownloadOutlined />}
              loading={exporting}
              onClick={async () => {
                setExporting(true);
                try {
                  await trainApi.exportFollowTaskAnswers(taskId);
                  message.success('已开始下载');
                } catch (e) {
                  message.error(e instanceof Error ? e.message : '导出失败');
                } finally {
                  setExporting(false);
                }
              }}
            >
              导出答卷
            </Button>
          ) : null
        }
      >
        <Space size="large" style={{ marginBottom: 12 }} wrap>
          <Statistic title="平均分" value={stats?.avgScore ?? '—'} loading={statsLoading} />
          <Statistic
            title="达标率"
            value={stats?.passRate ?? '—'}
            suffix={stats?.passRate != null ? '%' : undefined}
            loading={statsLoading}
          />
          <Statistic
            title="平均正确率"
            value={stats?.avgAccuracyRate ?? '—'}
            suffix={stats?.avgAccuracyRate != null ? '%' : undefined}
            loading={statsLoading}
          />
          <Statistic
            title="平均时长"
            value={formatDuration(stats?.avgDurationSeconds)}
            loading={statsLoading}
          />
        </Space>

        {weakQuestions.length > 0 ? (
          <div style={{ marginBottom: 12 }}>
            <Typography.Text type="secondary">薄弱题（正确率最低）</Typography.Text>
            <Table
              style={{ marginTop: 8 }}
              rowKey={(r) => String(r.questionId)}
              size="small"
              pagination={false}
              columns={qStatColumns}
              dataSource={weakQuestions}
            />
          </div>
        ) : null}

        <Table
          rowKey={(r) => String(r.id)}
          size="small"
          loading={itemsLoading}
          columns={itemColumns}
          dataSource={items ?? []}
          pagination={false}
        />
      </Drawer>

      <Drawer
        title={`答卷 — ${answerItem?.userName || ''}`}
        width={560}
        open={!!answerItem}
        onClose={() => setAnswerItem(null)}
      >
        {answersLoading ? (
          <Typography.Text type="secondary">加载中…</Typography.Text>
        ) : answers ? (
          <>
            <Descriptions size="small" column={2} style={{ marginBottom: 12 }}>
              <Descriptions.Item label="得分">{answers.score ?? '—'}</Descriptions.Item>
              <Descriptions.Item label="达标">
                {answers.passed == null ? '—' : answers.passed ? '是' : '否'}
              </Descriptions.Item>
              <Descriptions.Item label="正确率">
                {answers.accuracyRate != null
                  ? `${answers.accuracyRate}%`
                  : answerItem?.accuracyRate != null
                    ? `${answerItem.accuracyRate}%`
                    : '—'}
              </Descriptions.Item>
              <Descriptions.Item label="时长">
                {formatDuration(
                  answers.durationSeconds != null
                    ? answers.durationSeconds
                    : answerItem?.durationSeconds,
                )}
              </Descriptions.Item>
            </Descriptions>
            {(answers.answers ?? []).map((a) => (
              <div
                key={a.questionId}
                style={{
                  marginBottom: 12,
                  padding: 12,
                  background: '#fafafa',
                  borderRadius: 8,
                  border: `1px solid ${a.correct ? '#b7eb8f' : '#ffccc7'}`,
                }}
              >
                <Typography.Text strong>
                  {a.sortNumber}. [{a.questionType}] {stripHtml(a.stem)}
                </Typography.Text>
                <div style={{ marginTop: 8 }}>
                  <Typography.Text type="secondary">你的选择：</Typography.Text>{' '}
                  <Typography.Text>{formatAnswerJson(a.userAnswerJson)}</Typography.Text>
                </div>
                <div>
                  <Typography.Text type="secondary">正确答案：</Typography.Text>{' '}
                  <Typography.Text type="success">
                    {formatAnswerJson(a.correctAnswerJson)}
                  </Typography.Text>
                </div>
                <div style={{ marginTop: 6 }}>
                  <Tag color={a.correct ? 'green' : 'red'}>{a.correct ? '正确' : '错误'}</Tag>
                  <Typography.Text type="secondary">
                    {' '}
                    {a.score ?? 0}/{a.fullScore ?? 0}
                  </Typography.Text>
                </div>
                {a.analysis ? (
                  <div style={{ marginTop: 4 }}>
                    <Typography.Text type="secondary">解析：{stripHtml(a.analysis)}</Typography.Text>
                  </div>
                ) : null}
              </div>
            ))}
          </>
        ) : (
          <Empty />
        )}
      </Drawer>
    </div>
  );
}

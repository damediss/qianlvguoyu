import { useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Checkbox,
  Descriptions,
  Drawer,
  Empty,
  Form,
  Input,
  InputNumber,
  Modal,
  Progress,
  Select,
  Space,
  Statistic,
  Switch,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import { DownloadOutlined, PlusOutlined, SyncOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as trainApi from '../../../api/training';
import {
  MakeExamPlanForm,
  buildMakeExamPlanCreatePayload,
} from '../../exam/MakeExamPlanForm';

type Props = { classId: number };

/** 模拟考实时刷新间隔（毫秒），对齐竞赛理论监考 */
const LIVE_POLL_MS = 5000;

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

function summarizeImport(stats?: trainApi.MockPlanAttachResult['importStats']) {
  if (!stats) return '';
  const parts = [`已挂接客观题 ${stats.importedCount ?? 0} 道`];
  if ((stats.subjectiveSkipped ?? 0) > 0) {
    parts.push(`跳过非客观题 ${stats.subjectiveSkipped}`);
  }
  if ((stats.excludedByOtherMock ?? 0) > 0) {
    parts.push(`排除他卷占用 ${stats.excludedByOtherMock}`);
  }
  return parts.join('；');
}

export function FollowMockExamTab({ classId }: Props) {
  const queryClient = useQueryClient();
  const [taskId, setTaskId] = useState<string | number | null>(null);
  const [answerItem, setAnswerItem] = useState<trainApi.FollowTaskItem | null>(null);
  const [addPlanOpen, setAddPlanOpen] = useState(false);
  const [pickPlanOpen, setPickPlanOpen] = useState(false);
  const [addForm] = Form.useForm();
  const [pickForm] = Form.useForm();
  const [exporting, setExporting] = useState(false);
  /** 实时刷新：列表 + 打开的答卷详情/学员答卷 */
  const [liveRefresh, setLiveRefresh] = useState(false);
  const [lastLiveAt, setLastLiveAt] = useState<Date | null>(null);

  const modalBusy = addPlanOpen || pickPlanOpen;
  const pollMs = liveRefresh && !modalBusy ? LIVE_POLL_MS : false;

  const { data: usedMeta } = useQuery({
    queryKey: ['mock-used-q', classId],
    queryFn: () => trainApi.getMockUsedQuestionIds(classId),
    enabled: classId > 0,
  });

  const { data: bankScope } = useQuery({
    queryKey: ['follow-bank-scope', classId],
    queryFn: () => trainApi.getFollowBankScope(classId),
    enabled: classId > 0,
  });

  const { data: planOptions, isLoading: planOptionsLoading } = useQuery({
    queryKey: ['mock-plan-options', classId],
    queryFn: () => trainApi.listMockPlanOptions(classId),
    enabled: classId > 0 && pickPlanOpen,
  });

  const {
    data: tasks,
    isLoading,
    isFetching: tasksFetching,
    dataUpdatedAt: tasksUpdatedAt,
    refetch,
  } = useQuery({
    queryKey: ['follow-tasks', classId, 'mock'],
    queryFn: () => trainApi.listFollowTasks(classId, 'mock'),
    enabled: classId > 0,
    refetchInterval: pollMs,
    refetchIntervalInBackground: false,
  });

  const selectedTask = useMemo(
    () => (tasks || []).find((t) => String(t.id) === String(taskId)) ?? null,
    [tasks, taskId],
  );

  const {
    data: items,
    isLoading: itemsLoading,
    isFetching: itemsFetching,
    dataUpdatedAt: itemsUpdatedAt,
  } = useQuery({
    queryKey: ['follow-task-items', taskId],
    queryFn: () => trainApi.listFollowTaskItems(taskId!),
    enabled: taskId != null,
    refetchInterval: taskId != null ? pollMs : false,
    refetchIntervalInBackground: false,
  });

  const {
    data: stats,
    isLoading: statsLoading,
    isFetching: statsFetching,
    dataUpdatedAt: statsUpdatedAt,
  } = useQuery({
    queryKey: ['follow-task-stats', taskId],
    queryFn: () => trainApi.getFollowTaskStats(taskId!),
    enabled: taskId != null,
    refetchInterval: taskId != null ? pollMs : false,
    refetchIntervalInBackground: false,
  });

  const weakQuestions = useMemo(() => {
    const list = stats?.questionStats ?? [];
    return [...list]
      .filter((q) => q.accuracyRate != null)
      .sort((a, b) => (a.accuracyRate ?? 100) - (b.accuracyRate ?? 100));
  }, [stats]);

  const {
    data: answers,
    isLoading: answersLoading,
    isFetching: answersFetching,
    dataUpdatedAt: answersUpdatedAt,
  } = useQuery({
    queryKey: ['follow-task-answers', taskId, answerItem?.id],
    queryFn: () => trainApi.getFollowTaskItemAnswers(taskId!, answerItem!.id),
    enabled: taskId != null && answerItem != null,
    refetchInterval: taskId != null && answerItem != null ? pollMs : false,
    refetchIntervalInBackground: false,
  });

  // 明细刷新后同步当前选中学员行（状态/得分变化）
  useEffect(() => {
    if (!answerItem || !items?.length) return;
    const latest = items.find((i) => String(i.id) === String(answerItem.id));
    if (!latest) return;
    if (
      latest.status !== answerItem.status ||
      latest.score !== answerItem.score ||
      latest.submittedAt !== answerItem.submittedAt ||
      latest.accuracyRate !== answerItem.accuracyRate ||
      latest.correctCount !== answerItem.correctCount
    ) {
      setAnswerItem(latest);
    }
  }, [items, answerItem]);

  useEffect(() => {
    const stamps = [tasksUpdatedAt, itemsUpdatedAt, statsUpdatedAt, answersUpdatedAt].filter(
      (t) => t != null && t > 0,
    );
    if (!stamps.length) return;
    setLastLiveAt(new Date(Math.max(...stamps)));
  }, [tasksUpdatedAt, itemsUpdatedAt, statsUpdatedAt, answersUpdatedAt]);

  const liveFetching =
    liveRefresh && (tasksFetching || itemsFetching || statsFetching || answersFetching);

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['follow-tasks', classId, 'mock'] });
    void queryClient.invalidateQueries({ queryKey: ['mock-used-q', classId] });
    void queryClient.invalidateQueries({ queryKey: ['mock-plan-options', classId] });
    if (taskId != null) {
      void queryClient.invalidateQueries({ queryKey: ['follow-task-items', taskId] });
      void queryClient.invalidateQueries({ queryKey: ['follow-task-stats', taskId] });
    }
  };

  const addPlanMu = useMutation({
    mutationFn: async () => {
      const v = await addForm.validateFields();
      const plan = buildMakeExamPlanCreatePayload(v);
      plan.examType = 0;
      return trainApi.createMockWithPlan(classId, {
        plan,
        title: plan.title,
        passScore: plan.passPoint,
        showAnalysisAfterSubmit: false,
        excludeOtherMock: true,
      });
    },
    onSuccess: (res) => {
      message.success(`模拟考草稿已创建（${summarizeImport(res.importStats) || '已挂接'}）`);
      setAddPlanOpen(false);
      addForm.resetFields();
      invalidate();
    },
    onError: (e: Error) => message.error(e.message || '添加组卷失败'),
  });

  const pickPlanMu = useMutation({
    mutationFn: async () => {
      const v = await pickForm.validateFields();
      return trainApi.createMockFromPlan(classId, {
        makeExamPlanId: v.makeExamPlanId,
        title: v.title || undefined,
        description: v.description,
        bizDate: v.bizDate || undefined,
        passScore: v.passScore,
        showAnalysisAfterSubmit: !!v.showAnalysisAfterSubmit,
        excludeOtherMock: !!v.excludeOtherMock,
      });
    },
    onSuccess: (res) => {
      message.success(`模拟考草稿已创建（${summarizeImport(res.importStats) || '已挂接'}）`);
      setPickPlanOpen(false);
      pickForm.resetFields();
      invalidate();
    },
    onError: (e: Error) => message.error(e.message || '选用计划失败'),
  });

  const publishMu = useMutation({
    mutationFn: (id: string | number) => trainApi.publishFollowTask(id),
    onSuccess: () => {
      message.success('已发布');
      invalidate();
    },
    onError: (e: Error) => message.error(e.message || '发布失败'),
  });

  const closeMu = useMutation({
    mutationFn: (id: string | number) => trainApi.closeFollowTask(id),
    onSuccess: () => {
      message.success('已关闭');
      invalidate();
    },
    onError: (e: Error) => message.error(e.message || '关闭失败'),
  });

  const reopenMu = useMutation({
    mutationFn: (id: string | number) => trainApi.reopenFollowTask(id),
    onSuccess: () => {
      message.success('已重新打开');
      invalidate();
    },
    onError: (e: Error) => message.error(e.message || '重新打开失败'),
  });

  const planCourses = useMemo(
    () =>
      (bankScope?.courses || []).map((c) => ({
        id: c.courseId,
        title: c.title,
      })),
    [bankScope],
  );

  const taskColumns: ColumnsType<trainApi.FollowTask> = [
    { title: '标题', dataIndex: 'title', ellipsis: true },
    {
      title: '来源计划',
      width: 160,
      ellipsis: true,
      render: (_, r) => r.makeExamPlanTitle || (r.makeExamPlanId != null ? `#${r.makeExamPlanId}` : '—'),
    },
    {
      title: '题量',
      width: 70,
      render: (_, r) => r.questionCount ?? '—',
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
      width: 240,
      render: (_, r) => (
        <Space size="small" wrap>
          <Button
            type="link"
            size="small"
            onClick={() => {
              setTaskId(r.id);
              setAnswerItem(null);
            }}
          >
            答卷详情
          </Button>
          {r.status === 'draft' ? (
            <Button
              type="link"
              size="small"
              loading={publishMu.isPending}
              onClick={() => publishMu.mutate(r.id)}
            >
              发布
            </Button>
          ) : null}
          {r.status === 'published' ? (
            <Button
              type="link"
              size="small"
              danger
              loading={closeMu.isPending}
              onClick={() => closeMu.mutate(r.id)}
            >
              关闭
            </Button>
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
      title: '正确题数',
      width: 90,
      render: (_, r) =>
        r.status === 'pending'
          ? '—'
          : `${r.correctCount ?? 0}/${r.questionCount ?? '—'}`,
    },
    {
      title: '正确率',
      width: 90,
      render: (_, r) => (r.accuracyRate != null ? `${r.accuracyRate}%` : '—'),
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
      width: 100,
      render: (_, r) =>
        r.status !== 'pending' ? (
          <Button type="link" size="small" onClick={() => setAnswerItem(r)}>
            答卷详情
          </Button>
        ) : (
          <Typography.Text type="secondary">未交</Typography.Text>
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
    <Space direction="vertical" style={{ width: '100%' }} size="middle">
      <Alert
        type="info"
        showIcon
        message="班级模拟考：通过题库「组卷计划」添加组卷或选用已有计划；开启实时更新后可自动刷新交卷进度与答卷详情。"
        description={
          usedMeta != null
            ? `本班其他模拟卷已占用题库题 ${usedMeta.count ?? 0} 道（草稿+已发布）`
            : undefined
        }
      />
      <Space wrap align="center" style={{ width: '100%', justifyContent: 'space-between' }}>
        <Space wrap>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => {
              addForm.resetFields();
              addForm.setFieldsValue({
                examType: 0,
                isAll: 0,
                isComputer: true,
                isPrint: false,
                courseId: planCourses[0]?.id,
              });
              setAddPlanOpen(true);
            }}
          >
            添加组卷
          </Button>
          <Button
            onClick={() => {
              pickForm.setFieldsValue({
                excludeOtherMock: true,
                showAnalysisAfterSubmit: false,
              });
              setPickPlanOpen(true);
            }}
          >
            选用已有计划
          </Button>
          <Button
            icon={<SyncOutlined spin={tasksFetching} />}
            onClick={() => {
              void refetch();
              if (taskId != null) {
                void queryClient.invalidateQueries({ queryKey: ['follow-task-items', taskId] });
                void queryClient.invalidateQueries({ queryKey: ['follow-task-stats', taskId] });
              }
            }}
          >
            刷新
          </Button>
        </Space>
        <Space size="middle" align="center">
          {liveRefresh && lastLiveAt ? (
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              {liveFetching ? '同步中…' : `最近更新 ${lastLiveAt.toLocaleTimeString()}`}
            </Typography.Text>
          ) : null}
          <Space size={6} align="center">
            <Typography.Text>实时更新</Typography.Text>
            <Switch
              checked={liveRefresh}
              onChange={setLiveRefresh}
              checkedChildren="开"
              unCheckedChildren="关"
            />
          </Space>
        </Space>
      </Space>

      <Table
        rowKey={(r) => String(r.id)}
        loading={isLoading}
        columns={taskColumns}
        dataSource={tasks || []}
        pagination={false}
        size="middle"
        locale={{ emptyText: <Empty description="暂无模拟卷，请添加组卷或选用已有计划" /> }}
      />

      <Drawer
        title={
          <Space>
            <span>
              {selectedTask
                ? `答卷详情 — ${selectedTask.title}`
                : '答卷详情'}
            </span>
            {liveRefresh ? (
              <Tag color="processing" icon={<SyncOutlined spin={itemsFetching || statsFetching} />}>
                实时
              </Tag>
            ) : null}
          </Space>
        }
        open={taskId != null}
        onClose={() => {
          setTaskId(null);
          setAnswerItem(null);
        }}
        width={900}
        destroyOnHidden
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
        {selectedTask?.makeExamPlanTitle || selectedTask?.makeExamPlanId != null ? (
          <Alert
            type="success"
            showIcon
            style={{ marginBottom: 12 }}
            message={`来源组卷计划：${selectedTask.makeExamPlanTitle || `#${selectedTask.makeExamPlanId}`}`}
          />
        ) : null}

        <Space size="large" style={{ marginBottom: 12 }} wrap>
          <Statistic title="应交" value={stats?.totalCount ?? 0} loading={statsLoading} />
          <Statistic title="已交" value={stats?.submittedCount ?? 0} loading={statsLoading} />
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
            <Typography.Text type="secondary">
              薄弱题（按正确率从低到高，共 {weakQuestions.length} 题）
            </Typography.Text>
            <Table
              style={{ marginTop: 8 }}
              rowKey={(r) => String(r.questionId)}
              size="small"
              pagination={false}
              columns={qStatColumns}
              dataSource={weakQuestions}
              scroll={{ y: 200 }}
            />
          </div>
        ) : null}

        <Table
          rowKey={(r) => String(r.id)}
          loading={itemsLoading}
          columns={itemColumns}
          dataSource={items || []}
          pagination={false}
          size="small"
          scroll={{ x: 900 }}
          locale={{
            emptyText: (
              <Empty
                description={
                  selectedTask?.status === 'draft'
                    ? '草稿尚未发布，暂无学员答卷'
                    : '暂无学员交卷记录'
                }
              />
            ),
          }}
        />
      </Drawer>

      <Drawer
        title={
          <Space>
            <span>{`答卷详情 — ${answerItem?.userName || ''}`}</span>
            {liveRefresh ? (
              <Tag color="processing" icon={<SyncOutlined spin={answersFetching} />}>
                实时
              </Tag>
            ) : null}
          </Space>
        }
        open={answerItem != null}
        onClose={() => setAnswerItem(null)}
        width={640}
        destroyOnHidden
      >
        {answersLoading ? (
          <Typography.Text type="secondary">加载中…</Typography.Text>
        ) : answers ? (
          <>
            {selectedTask?.makeExamPlanTitle || selectedTask?.makeExamPlanId != null ? (
              <Alert
                type="info"
                showIcon
                style={{ marginBottom: 12 }}
                message={`来源计划：${selectedTask.makeExamPlanTitle || `#${selectedTask.makeExamPlanId}`}`}
              />
            ) : null}
            <Descriptions size="small" column={2} style={{ marginBottom: 12 }}>
              <Descriptions.Item label="得分">{answers.score ?? '—'}</Descriptions.Item>
              <Descriptions.Item label="达标">
                {answers.passed == null ? '—' : answers.passed ? '是' : '否'}
              </Descriptions.Item>
              <Descriptions.Item label="正确题数">
                {answers.correctCount ?? 0}/{answers.questionCount ?? 0}
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
              <Descriptions.Item label="提交时间">
                {answerItem?.submittedAt
                  ? String(answerItem.submittedAt).replace('T', ' ').slice(0, 16)
                  : '—'}
              </Descriptions.Item>
            </Descriptions>
            {(answers.answers ?? []).length === 0 ? (
              <Empty description="暂无答题明细" />
            ) : (
              (answers.answers ?? []).map((a) => (
                <div
                  key={String(a.questionId)}
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
                    <Typography.Text type="secondary">学员作答：</Typography.Text>{' '}
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
                      <Typography.Text type="secondary">
                        解析：{stripHtml(a.analysis)}
                      </Typography.Text>
                    </div>
                  ) : null}
                </div>
              ))
            )}
          </>
        ) : (
          <Empty />
        )}
      </Drawer>

      <Modal
        title="添加组卷计划"
        open={addPlanOpen}
        onCancel={() => {
          setAddPlanOpen(false);
          addForm.resetFields();
        }}
        onOk={() => addPlanMu.mutate()}
        confirmLoading={addPlanMu.isPending}
        width={900}
        destroyOnHidden
        okText="生成计划并挂接模拟考"
      >
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
          message="与题库「组卷计划 → 添加」同一套表单与生成逻辑；提交后写入组卷计划列表，并自动挂接为本班模拟考草稿。"
        />
        <MakeExamPlanForm
          form={addForm}
          courses={planCourses}
          examTypeLocked={0}
          coursePlaceholder={
            planCourses.length ? '选择科目' : '暂无科目，请先配置本班练习范围'
          }
        />
      </Modal>

      <Modal
        title="选用已有组卷计划"
        open={pickPlanOpen}
        onCancel={() => setPickPlanOpen(false)}
        onOk={() => pickPlanMu.mutate()}
        confirmLoading={pickPlanMu.isPending}
        width={640}
        destroyOnHidden
        okText="挂接为模拟考草稿"
      >
        <Form form={pickForm} layout="vertical">
          <Form.Item
            name="makeExamPlanId"
            label="组卷计划"
            rules={[{ required: true, message: '请选择组卷计划' }]}
          >
            <Select
              loading={planOptionsLoading}
              showSearch
              optionFilterProp="label"
              placeholder="选择理论组卷计划"
              options={(planOptions || []).map((p) => ({
                value: p.id,
                label: `${p.title}（${p.courseTitle || p.courseId}）`,
              }))}
              onChange={(id: number) => {
                const opt = (planOptions || []).find((p) => p.id === id);
                if (opt) {
                  pickForm.setFieldsValue({
                    title: opt.title,
                    passScore: opt.passPoint ?? undefined,
                  });
                }
              }}
            />
          </Form.Item>
          <Form.Item name="title" label="模拟考标题">
            <Input maxLength={200} />
          </Form.Item>
          <Form.Item name="bizDate" label="业务日（可选）">
            <Input placeholder="YYYY-MM-DD" />
          </Form.Item>
          <Form.Item name="description" label="说明">
            <Input.TextArea rows={2} maxLength={1000} />
          </Form.Item>
          <Form.Item name="passScore" label="及格分（可选）">
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="showAnalysisAfterSubmit" label="交卷后显示解析" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item name="excludeOtherMock" valuePropName="checked">
            <Checkbox>挂接时排除本班其他模拟卷已选题</Checkbox>
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  );
}

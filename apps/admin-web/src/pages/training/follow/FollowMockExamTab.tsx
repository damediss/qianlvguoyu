import { useMemo, useState } from 'react';
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
import { DownloadOutlined } from '@ant-design/icons';
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

export function FollowMockExamTab({ classId }: Props) {
  const queryClient = useQueryClient();
  const [taskId, setTaskId] = useState<string | number | null>(null);
  const [answerItem, setAnswerItem] = useState<trainApi.FollowTaskItem | null>(null);
  const [editorOpen, setEditorOpen] = useState(false);
  const [drawnIds, setDrawnIds] = useState<number[]>([]);
  const [drawnPreview, setDrawnPreview] = useState<
    { id: number; questionType?: string; stemPreview?: string; score?: number }[]
  >([]);
  const [drawMeta, setDrawMeta] = useState<{
    drawn: Record<string, number>;
    shortage: Record<string, number>;
    typeCounts: Record<string, number>;
  } | null>(null);
  const [form] = Form.useForm();
  const [exporting, setExporting] = useState(false);
  const watchedCourseId = Form.useWatch('courseId', form) as number | undefined;
  const watchedExcludeOther = Form.useWatch('excludeOtherMock', form) as boolean | undefined;

  const { data: usedMeta } = useQuery({
    queryKey: ['mock-used-q', classId],
    queryFn: () => trainApi.getMockUsedQuestionIds(classId),
    enabled: classId > 0,
  });

  const { data: bankScope } = useQuery({
    queryKey: ['follow-bank-scope', classId],
    queryFn: () => trainApi.getFollowBankScope(classId),
    enabled: classId > 0 && editorOpen,
  });

  const {
    data: poolTypeCounts,
    isFetching: poolCountsLoading,
  } = useQuery({
    queryKey: [
      'follow-bank-type-counts',
      classId,
      watchedCourseId,
      !!watchedExcludeOther,
    ],
    queryFn: () =>
      trainApi.getFollowBankTypeCounts(classId, {
        courseId: watchedCourseId!,
        excludeOtherMock: !!watchedExcludeOther,
      }),
    enabled: classId > 0 && editorOpen && watchedCourseId != null && watchedCourseId > 0,
  });

  const poolCounts = poolTypeCounts?.typeCounts;

  const { data: tasks, isLoading, refetch } = useQuery({
    queryKey: ['follow-tasks', classId, 'mock'],
    queryFn: () => trainApi.listFollowTasks(classId, 'mock'),
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

  const weakQuestions = useMemo(() => {
    const list = stats?.questionStats ?? [];
    return [...list]
      .filter((q) => q.accuracyRate != null)
      .sort((a, b) => (a.accuracyRate ?? 100) - (b.accuracyRate ?? 100))
      .slice(0, 5);
  }, [stats]);

  const { data: answers, isLoading: answersLoading } = useQuery({
    queryKey: ['follow-task-answers', taskId, answerItem?.id],
    queryFn: () => trainApi.getFollowTaskItemAnswers(taskId!, answerItem!.id),
    enabled: taskId != null && answerItem != null,
  });

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ['follow-tasks', classId, 'mock'] });
    void queryClient.invalidateQueries({ queryKey: ['mock-used-q', classId] });
  };

  const createMu = useMutation({
    mutationFn: async () => {
      const v = await form.validateFields();
      if (!drawnIds.length) throw new Error('请先智能抽题');
      return trainApi.createFollowTask({
        classId,
        taskType: 'mock',
        title: v.title,
        description: v.description,
        bizDate: v.bizDate || undefined,
        passScore: v.passScore,
        showAnalysisAfterSubmit: !!v.showAnalysisAfterSubmit,
        sourceExamDetailIds: drawnIds,
      });
    },
    onSuccess: () => {
      message.success('模拟卷草稿已创建');
      setEditorOpen(false);
      setDrawnIds([]);
      setDrawnPreview([]);
      setDrawMeta(null);
      form.resetFields();
      invalidate();
    },
    onError: (e: Error) => message.error(e.message || '创建失败'),
  });

  const drawMu = useMutation({
    mutationFn: async () => {
      const v = await form.validateFields(['courseId', 'singleCount', 'multiCount', 'judgeCount']);
      return trainApi.drawFollowBankQuestions(classId, {
        courseId: v.courseId,
        singleCount: v.singleCount ?? 0,
        multiCount: v.multiCount ?? 0,
        judgeCount: v.judgeCount ?? 0,
        excludeOtherMock: !!form.getFieldValue('excludeOtherMock'),
      });
    },
    onSuccess: (res) => {
      const ids = (res.items || []).map((i) => i.id).filter(Boolean);
      setDrawnIds(ids);
      setDrawnPreview(res.items || []);
      setDrawMeta({
        drawn: {
          single: Number(res.drawn?.single ?? 0),
          multi: Number(res.drawn?.multi ?? 0),
          judge: Number(res.drawn?.judge ?? 0),
        },
        shortage: {
          single: Number(res.shortage?.single ?? 0),
          multi: Number(res.shortage?.multi ?? 0),
          judge: Number(res.shortage?.judge ?? 0),
        },
        typeCounts: {
          single: Number(res.typeCounts?.single ?? 0),
          multi: Number(res.typeCounts?.multi ?? 0),
          judge: Number(res.typeCounts?.judge ?? 0),
        },
      });
      if (!ids.length) {
        message.warning(res.hint || '未抽到题目，请调整范围或取消「未抽题」');
      } else {
        message.success(res.hint || `已抽 ${ids.length} 题`);
      }
    },
    onError: (e: Error) => message.error(e.message || '抽题失败'),
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

  const courseOptions = useMemo(
    () =>
      (bankScope?.courses || []).map((c) => ({
        value: c.courseId,
        label: c.title,
      })),
    [bankScope],
  );

  const taskColumns: ColumnsType<trainApi.FollowTask> = [
    { title: '标题', dataIndex: 'title', ellipsis: true },
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
      width: 220,
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
            明细
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
    <Space direction="vertical" style={{ width: '100%' }} size="middle">
      <Alert
        type="info"
        showIcon
        message="班级模拟考：同卷统考；配卷时可排除本班其他模拟卷已抽题。答卷落跟班任务表，与班级刷题分离。"
        description={
          usedMeta != null
            ? `本班其他模拟卷已占用题库题 ${usedMeta.count ?? 0} 道（草稿+已发布）`
            : undefined
        }
      />
      <Space>
        <Button
          type="primary"
          onClick={() => {
            form.setFieldsValue({
              title: '模拟考',
              singleCount: 10,
              multiCount: 5,
              judgeCount: 5,
              excludeOtherMock: true,
              showAnalysisAfterSubmit: false,
            });
            setDrawnIds([]);
            setDrawnPreview([]);
            setDrawMeta(null);
            setEditorOpen(true);
          }}
        >
          新建模拟卷
        </Button>
        <Button onClick={() => void refetch()}>刷新</Button>
      </Space>

      <Table
        rowKey={(r) => String(r.id)}
        loading={isLoading}
        columns={taskColumns}
        dataSource={tasks || []}
        pagination={false}
        size="middle"
        locale={{ emptyText: <Empty description="暂无模拟卷" /> }}
      />

      <Drawer
        title="模拟考提交明细"
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
          loading={itemsLoading}
          columns={itemColumns}
          dataSource={items || []}
          pagination={false}
          size="small"
          scroll={{ x: 900 }}
        />
      </Drawer>

      <Drawer
        title={`答卷 — ${answerItem?.userName || ''}`}
        open={answerItem != null}
        onClose={() => setAnswerItem(null)}
        width={640}
        destroyOnHidden
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
            {(answers.answers ?? []).map((a) => (
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
                    <Typography.Text type="secondary">
                      解析：{stripHtml(a.analysis)}
                    </Typography.Text>
                  </div>
                ) : null}
              </div>
            ))}
          </>
        ) : (
          <Empty />
        )}
      </Drawer>

      <Modal
        title="新建模拟卷"
        open={editorOpen}
        onCancel={() => setEditorOpen(false)}
        onOk={() => createMu.mutate()}
        confirmLoading={createMu.isPending}
        width={640}
        destroyOnHidden
        okText="保存草稿"
      >
        <Form form={form} layout="vertical">
          <Form.Item name="title" label="标题" rules={[{ required: true, message: '请输入标题' }]}>
            <Input placeholder="如：一模" maxLength={200} />
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
            <Checkbox>抽题时排除本班其他模拟卷已抽题</Checkbox>
          </Form.Item>
          <Form.Item name="courseId" label="科目" rules={[{ required: true, message: '请选择科目' }]}>
            <Select
              options={courseOptions}
              placeholder={courseOptions.length ? '选择科目' : '暂无科目，请先配置练习范围'}
              allowClear
              onChange={() => {
                setDrawnIds([]);
                setDrawnPreview([]);
                setDrawMeta(null);
              }}
            />
          </Form.Item>

          {watchedCourseId ? (
            <Alert
              type="success"
              showIcon
              style={{ marginBottom: 16 }}
              message="当前科目可抽题型题数"
              description={
                poolCountsLoading ? (
                  <Typography.Text type="secondary">统计中…</Typography.Text>
                ) : (
                  <Space size="large" wrap style={{ marginTop: 8 }}>
                    <Statistic title="单选" value={Number(poolCounts?.single ?? 0)} />
                    <Statistic title="多选" value={Number(poolCounts?.multi ?? 0)} />
                    <Statistic title="判断" value={Number(poolCounts?.judge ?? 0)} />
                    <Statistic title="合计" value={Number(poolCounts?.total ?? 0)} />
                  </Space>
                )
              }
            />
          ) : (
            <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 12 }}>
              请先选择科目，将显示该科目下单选/多选/判断可抽题数
            </Typography.Text>
          )}

          <Typography.Text strong style={{ display: 'block', marginBottom: 8 }}>
            抽取配置
          </Typography.Text>
          <Space wrap size="middle" style={{ marginBottom: 8 }}>
            <Form.Item name="singleCount" label="单选" style={{ marginBottom: 0 }}>
              <InputNumber min={0} max={50} />
            </Form.Item>
            <Form.Item name="multiCount" label="多选" style={{ marginBottom: 0 }}>
              <InputNumber min={0} max={50} />
            </Form.Item>
            <Form.Item name="judgeCount" label="判断" style={{ marginBottom: 0 }}>
              <InputNumber min={0} max={50} />
            </Form.Item>
            <Button
              type="primary"
              loading={drawMu.isPending}
              disabled={!watchedCourseId}
              onClick={() => drawMu.mutate()}
            >
              智能抽题
            </Button>
          </Space>
          {drawMeta ? (
            <Alert
              type="info"
              showIcon
              style={{ marginBottom: 12 }}
              message={
                <Space wrap size="middle">
                  <span>
                    单选 已抽 <b>{drawMeta.drawn.single}</b>
                    {drawMeta.shortage.single > 0 ? (
                      <Typography.Text type="danger">（缺 {drawMeta.shortage.single}）</Typography.Text>
                    ) : null}
                  </span>
                  <span>
                    多选 已抽 <b>{drawMeta.drawn.multi}</b>
                    {drawMeta.shortage.multi > 0 ? (
                      <Typography.Text type="danger">（缺 {drawMeta.shortage.multi}）</Typography.Text>
                    ) : null}
                  </span>
                  <span>
                    判断 已抽 <b>{drawMeta.drawn.judge}</b>
                    {drawMeta.shortage.judge > 0 ? (
                      <Typography.Text type="danger">（缺 {drawMeta.shortage.judge}）</Typography.Text>
                    ) : null}
                  </span>
                  <span>
                    合计 <b>{drawnIds.length}</b> 题
                  </span>
                </Space>
              }
            />
          ) : null}
          <div>
            <Typography.Text type="secondary">
              题目预览{drawnPreview.length ? `（${drawnPreview.length} 题）` : ''}
            </Typography.Text>
            {drawnPreview.length === 0 ? (
              <div style={{ color: '#999', fontSize: 12, marginTop: 4 }}>尚未抽题</div>
            ) : (
              <div
                style={{
                  marginTop: 8,
                  maxHeight: 220,
                  overflowY: 'auto',
                  paddingRight: 8,
                  border: '1px solid #f0f0f0',
                  borderRadius: 6,
                  background: '#fafafa',
                }}
              >
                {drawnPreview.map((q, idx) => (
                  <div
                    key={q.id}
                    style={{
                      fontSize: 12,
                      color: '#666',
                      padding: '6px 10px',
                      borderBottom: idx === drawnPreview.length - 1 ? 'none' : '1px solid #f0f0f0',
                    }}
                  >
                    {idx + 1}. #{q.id} [
                    {q.questionType === 'single'
                      ? '单选'
                      : q.questionType === 'multi'
                        ? '多选'
                        : q.questionType === 'judge'
                          ? '判断'
                          : q.questionType}
                    ] {q.stemPreview}
                  </div>
                ))}
              </div>
            )}
          </div>
        </Form>
      </Modal>
    </Space>
  );
}

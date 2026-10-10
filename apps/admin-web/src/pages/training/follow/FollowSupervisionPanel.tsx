/**
 * 督导检查单 + 整改闭环 + 满意度录入（L5 / M4）
 */
import { useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Col,
  DatePicker,
  Drawer,
  Form,
  Input,
  Modal,
  Radio,
  Row,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  CheckOutlined,
  CloseOutlined,
  PlusOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs, { type Dayjs } from 'dayjs';
import * as trainApi from '../../../api/training';
import type { FollowIssue, FollowSupervision, FollowSupervisionItem } from '../../../api/training';

type Props = {
  classId: number;
  bizDate?: Dayjs | null;
};

const RESULT_OPTIONS = [
  { value: 'pass', label: '合格' },
  { value: 'fail', label: '不合格' },
  { value: 'na', label: '不适用' },
];

const CONCLUSION_OPTIONS = [
  { value: 'pass', label: '通过' },
  { value: 'conditional', label: '有条件通过' },
  { value: 'fail', label: '不通过' },
];

function supervisionStatusTag(status?: string) {
  if (status === 'draft') return <Tag>草稿</Tag>;
  if (status === 'submitted') return <Tag color="processing">已提交</Tag>;
  if (status === 'closed') return <Tag color="success">已关闭</Tag>;
  return <Tag>{status || '—'}</Tag>;
}

function issueStatusTag(status?: string) {
  if (status === 'open') return <Tag color="error">待处理</Tag>;
  if (status === 'doing') return <Tag color="processing">整改中</Tag>;
  if (status === 'done') return <Tag color="warning">待复核</Tag>;
  if (status === 'closed') return <Tag color="success">已关闭</Tag>;
  return <Tag>{status || '—'}</Tag>;
}

function severityTag(level?: string) {
  if (level === 'critical') return <Tag color="error">严重</Tag>;
  if (level === 'warn') return <Tag color="warning">警示</Tag>;
  return <Tag>信息</Tag>;
}

function nextIssueActions(status?: string): Array<{ status: string; label: string }> {
  if (status === 'open') return [{ status: 'doing', label: '开始整改' }, { status: 'closed', label: '直接关闭' }];
  if (status === 'doing') return [{ status: 'done', label: '完成整改' }];
  if (status === 'done') return [{ status: 'closed', label: '复核关闭' }];
  return [];
}

export function FollowSupervisionPanel({ classId, bizDate }: Props) {
  const qc = useQueryClient();
  const dayStr = bizDate ? bizDate.format('YYYY-MM-DD') : undefined;
  const [activeId, setActiveId] = useState<string | number | null>(null);
  const [issueModalOpen, setIssueModalOpen] = useState(false);
  const [surveyModalOpen, setSurveyModalOpen] = useState(false);
  const [issueForm] = Form.useForm();
  const [surveyForm] = Form.useForm();
  const [submitForm] = Form.useForm();

  const listQuery = useQuery({
    queryKey: ['follow-supervisions', classId, dayStr],
    queryFn: () => trainApi.listFollowSupervisions(classId, { bizDate: dayStr }),
  });

  const detailQuery = useQuery({
    queryKey: ['follow-supervision', activeId],
    queryFn: () => trainApi.getFollowSupervision(activeId!),
    enabled: activeId != null,
  });

  const issuesQuery = useQuery({
    queryKey: ['follow-issues', classId],
    queryFn: () => trainApi.listFollowIssues(classId),
  });

  const surveysQuery = useQuery({
    queryKey: ['follow-surveys', classId],
    queryFn: () => trainApi.listFollowSurveys(classId),
  });

  const invalidate = () => {
    void qc.invalidateQueries({ queryKey: ['follow-supervisions', classId] });
    void qc.invalidateQueries({ queryKey: ['follow-supervision'] });
    void qc.invalidateQueries({ queryKey: ['follow-issues', classId] });
    void qc.invalidateQueries({ queryKey: ['follow-surveys', classId] });
    void qc.invalidateQueries({ queryKey: ['follow-op-logs', classId] });
    void qc.invalidateQueries({ queryKey: ['follow-quality-metrics', classId] });
  };

  const createMut = useMutation({
    mutationFn: () =>
      trainApi.createFollowSupervision(classId, {
        bizDate: dayStr || dayjs().format('YYYY-MM-DD'),
      }),
    onSuccess: (row) => {
      message.success('已创建督导检查草稿');
      setActiveId(row.id);
      invalidate();
    },
    onError: (e: Error) => message.error(e.message || '创建失败'),
  });

  const patchItemMut = useMutation({
    mutationFn: ({ itemId, payload }: { itemId: number | string; payload: { result?: string; remark?: string } }) =>
      trainApi.patchFollowSupervisionItem(itemId, payload),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['follow-supervision', activeId] });
    },
    onError: (e: Error) => message.error(e.message || '保存检查项失败'),
  });

  const submitMut = useMutation({
    mutationFn: (payload: { conclusion?: string; remark?: string }) =>
      trainApi.submitFollowSupervision(activeId!, payload),
    onSuccess: () => {
      message.success('已提交；不合格项已自动生成整改单');
      invalidate();
    },
    onError: (e: Error) => message.error(e.message || '提交失败'),
  });

  const closeMut = useMutation({
    mutationFn: () => trainApi.closeFollowSupervision(activeId!),
    onSuccess: () => {
      message.success('检查单已关闭');
      invalidate();
    },
    onError: (e: Error) => message.error(e.message || '关闭失败'),
  });

  const createIssueMut = useMutation({
    mutationFn: (payload: Parameters<typeof trainApi.createFollowIssue>[1]) =>
      trainApi.createFollowIssue(classId, payload),
    onSuccess: () => {
      message.success('整改单已开立');
      setIssueModalOpen(false);
      issueForm.resetFields();
      invalidate();
    },
    onError: (e: Error) => message.error(e.message || '开立失败'),
  });

  const transitionMut = useMutation({
    mutationFn: ({ id, status, resolveNote }: { id: number | string; status: string; resolveNote?: string }) =>
      trainApi.transitionFollowIssue(id, { status, resolveNote }),
    onSuccess: () => {
      message.success('状态已更新');
      invalidate();
    },
    onError: (e: Error) => message.error(e.message || '流转失败'),
  });

  const surveyMut = useMutation({
    mutationFn: (payload: Parameters<typeof trainApi.createFollowSurvey>[1]) =>
      trainApi.createFollowSurvey(classId, payload),
    onSuccess: () => {
      message.success('满意度已录入');
      setSurveyModalOpen(false);
      surveyForm.resetFields();
      invalidate();
    },
    onError: (e: Error) => message.error(e.message || '录入失败'),
  });

  const detail = detailQuery.data;
  const isDraft = detail?.status === 'draft';

  const listColumns: ColumnsType<FollowSupervision> = useMemo(
    () => [
      { title: '业务日', dataIndex: 'bizDate', width: 110 },
      {
        title: '检查人',
        dataIndex: 'inspectorName',
        width: 100,
        render: (v?: string) => v || '—',
      },
      {
        title: '状态',
        dataIndex: 'status',
        width: 100,
        render: (v?: string) => supervisionStatusTag(v),
      },
      {
        title: '结论',
        dataIndex: 'conclusion',
        width: 100,
        render: (v?: string) => {
          if (v === 'pass') return <Tag color="success">通过</Tag>;
          if (v === 'fail') return <Tag color="error">不通过</Tag>;
          if (v === 'conditional') return <Tag color="warning">有条件</Tag>;
          return '—';
        },
      },
      {
        title: '操作',
        width: 90,
        render: (_: unknown, row) => (
          <Button type="link" size="small" onClick={() => setActiveId(row.id)}>
            打开
          </Button>
        ),
      },
    ],
    [],
  );

  const itemColumns: ColumnsType<FollowSupervisionItem> = [
    { title: '检查项', dataIndex: 'itemTitle', ellipsis: true },
    {
      title: '结果',
      dataIndex: 'result',
      width: 150,
      render: (v: string | null | undefined, row) =>
        isDraft ? (
          <Select
            size="small"
            style={{ width: 120 }}
            value={v || undefined}
            placeholder="选择"
            options={RESULT_OPTIONS}
            onChange={(result) => patchItemMut.mutate({ itemId: row.id, payload: { result } })}
          />
        ) : (
          RESULT_OPTIONS.find((o) => o.value === v)?.label || '—'
        ),
    },
    {
      title: '备注',
      dataIndex: 'remark',
      render: (v: string | null | undefined, row) =>
        isDraft ? (
          <Input
            size="small"
            defaultValue={v || undefined}
            placeholder="可选"
            onBlur={(e) => {
              const next = e.target.value.trim();
              if (next !== (v || '')) {
                patchItemMut.mutate({ itemId: row.id, payload: { remark: next || undefined } });
              }
            }}
          />
        ) : (
          v || '—'
        ),
    },
  ];

  const issueColumns: ColumnsType<FollowIssue> = [
    {
      title: '严重度',
      dataIndex: 'severity',
      width: 80,
      render: (v?: string) => severityTag(v),
    },
    { title: '标题', dataIndex: 'title', ellipsis: true },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (v?: string) => issueStatusTag(v),
    },
    {
      title: '责任人',
      dataIndex: 'assigneeName',
      width: 90,
      render: (v?: string) => v || '—',
    },
    {
      title: '操作',
      width: 200,
      render: (_: unknown, row) => (
        <Space wrap size={0}>
          {nextIssueActions(row.status).map((a) => (
            <Button
              key={a.status}
              type="link"
              size="small"
              onClick={() => {
                if (a.status === 'closed' || a.status === 'done') {
                  Modal.confirm({
                    title: a.label,
                    content: (
                      <Input.TextArea
                        id={`issue-note-${row.id}`}
                        rows={3}
                        placeholder="处理说明（可选）"
                      />
                    ),
                    onOk: () => {
                      const el = document.getElementById(`issue-note-${row.id}`) as HTMLTextAreaElement | null;
                      transitionMut.mutate({
                        id: row.id,
                        status: a.status,
                        resolveNote: el?.value?.trim() || undefined,
                      });
                    },
                  });
                } else {
                  transitionMut.mutate({ id: row.id, status: a.status });
                }
              }}
            >
              {a.label}
            </Button>
          ))}
        </Space>
      ),
    },
  ];

  const avgSatisfaction = useMemo(() => {
    const rows = surveysQuery.data || [];
    if (!rows.length) return null;
    const sum = rows.reduce((acc, r) => acc + (Number(r.score) || 0), 0);
    return Math.round((sum / rows.length) * 100) / 100;
  }, [surveysQuery.data]);

  return (
    <Space direction="vertical" style={{ width: '100%' }} size="middle">
      <Alert
        type="info"
        showIcon
        message="督导检查：草稿勾选 → 提交（不合格自动开整改）→ 整改流转 → 关闭检查单"
        description="检查项默认含设备、纪律、到课、影像留痕、考勤、作业督促；提交会写入过程动态。"
      />

      <Card
        size="small"
        title="督导检查单"
        extra={
          <Space>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              loading={createMut.isPending}
              onClick={() => createMut.mutate()}
            >
              新建检查
            </Button>
            <Button icon={<ReloadOutlined />} onClick={() => void listQuery.refetch()}>
              刷新
            </Button>
          </Space>
        }
      >
        <Table
          rowKey="id"
          size="small"
          loading={listQuery.isLoading}
          dataSource={listQuery.data || []}
          columns={listColumns}
          pagination={{ pageSize: 8, showSizeChanger: false }}
          locale={{ emptyText: dayStr ? `${dayStr} 暂无督导检查` : '暂无督导检查' }}
        />
      </Card>

      <Row gutter={[12, 12]}>
        <Col xs={24} lg={16}>
          <Card
            size="small"
            title="整改闭环"
            extra={
              <Button size="small" icon={<PlusOutlined />} onClick={() => setIssueModalOpen(true)}>
                开立整改
              </Button>
            }
          >
            <Table
              rowKey="id"
              size="small"
              loading={issuesQuery.isLoading}
              dataSource={issuesQuery.data || []}
              columns={issueColumns}
              pagination={{ pageSize: 8, showSizeChanger: false }}
              locale={{ emptyText: '暂无整改单' }}
            />
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card
            size="small"
            title="满意度"
            extra={
              <Button size="small" onClick={() => setSurveyModalOpen(true)}>
                录入
              </Button>
            }
          >
            <Typography.Paragraph style={{ marginBottom: 8 }}>
              样本 {surveysQuery.data?.length ?? 0} 份 · 均分{' '}
              <Typography.Text strong>{avgSatisfaction ?? '—'}</Typography.Text>
            </Typography.Paragraph>
            <Table
              rowKey="id"
              size="small"
              loading={surveysQuery.isLoading}
              dataSource={(surveysQuery.data || []).slice(0, 5)}
              pagination={false}
              columns={[
                { title: '评分', dataIndex: 'score', width: 60 },
                {
                  title: '对象',
                  dataIndex: 'respondentName',
                  ellipsis: true,
                  render: (v?: string) => v || '匿名',
                },
              ]}
              locale={{ emptyText: '暂无答卷' }}
            />
          </Card>
        </Col>
      </Row>

      <Drawer
        title={detail ? `督导检查 · ${detail.bizDate}` : '督导检查'}
        open={activeId != null}
        onClose={() => setActiveId(null)}
        width={720}
        destroyOnHidden
        extra={
          detail ? (
            <Space>
              {supervisionStatusTag(detail.status)}
              {isDraft && (
                <Button
                  type="primary"
                  icon={<CheckOutlined />}
                  loading={submitMut.isPending}
                  onClick={() => {
                    submitForm.setFieldsValue({
                      conclusion: detail.conclusion || 'pass',
                      remark: detail.remark,
                    });
                    Modal.confirm({
                      title: '提交督导检查',
                      width: 480,
                      content: (
                        <Form form={submitForm} layout="vertical" style={{ marginTop: 12 }}>
                          <Form.Item name="conclusion" label="结论" rules={[{ required: true }]}>
                            <Radio.Group options={CONCLUSION_OPTIONS} />
                          </Form.Item>
                          <Form.Item name="remark" label="备注">
                            <Input.TextArea rows={2} />
                          </Form.Item>
                        </Form>
                      ),
                      onOk: async () => {
                        const values = await submitForm.validateFields();
                        await submitMut.mutateAsync(values);
                      },
                    });
                  }}
                >
                  提交
                </Button>
              )}
              {detail.status === 'submitted' && (
                <Button
                  icon={<CloseOutlined />}
                  loading={closeMut.isPending}
                  onClick={() => closeMut.mutate()}
                >
                  关闭检查单
                </Button>
              )}
            </Space>
          ) : null
        }
      >
        {detailQuery.isLoading ? (
          <Typography.Text type="secondary">加载中…</Typography.Text>
        ) : detail ? (
          <Space direction="vertical" style={{ width: '100%' }} size="middle">
            <Typography.Text type="secondary">
              检查人 {detail.inspectorName || '—'}
              {detail.openIssueCount != null ? ` · 关联未闭环整改 ${detail.openIssueCount}` : ''}
            </Typography.Text>
            <Table
              rowKey="id"
              size="small"
              dataSource={detail.items || []}
              columns={itemColumns}
              pagination={false}
            />
          </Space>
        ) : (
          <Typography.Text type="secondary">未找到检查单</Typography.Text>
        )}
      </Drawer>

      <Modal
        title="开立整改单"
        open={issueModalOpen}
        onCancel={() => setIssueModalOpen(false)}
        onOk={async () => {
          const values = await issueForm.validateFields();
          createIssueMut.mutate({
            title: values.title,
            description: values.description,
            severity: values.severity,
            assigneeName: values.assigneeName,
            dueDate: values.dueDate ? values.dueDate.format('YYYY-MM-DD') : undefined,
          });
        }}
        confirmLoading={createIssueMut.isPending}
        destroyOnHidden
      >
        <Form form={issueForm} layout="vertical" initialValues={{ severity: 'warn' }}>
          <Form.Item name="title" label="标题" rules={[{ required: true, message: '请输入标题' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="severity" label="严重度">
            <Select
              options={[
                { value: 'info', label: '信息' },
                { value: 'warn', label: '警示' },
                { value: 'critical', label: '严重' },
              ]}
            />
          </Form.Item>
          <Form.Item name="assigneeName" label="责任人">
            <Input />
          </Form.Item>
          <Form.Item name="dueDate" label="截止日期">
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="description" label="说明">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="录入满意度"
        open={surveyModalOpen}
        onCancel={() => setSurveyModalOpen(false)}
        onOk={async () => {
          const values = await surveyForm.validateFields();
          surveyMut.mutate({
            score: values.score,
            respondentName: values.respondentName,
            commentText: values.commentText,
            bizDate: dayStr || dayjs().format('YYYY-MM-DD'),
          });
        }}
        confirmLoading={surveyMut.isPending}
        destroyOnHidden
      >
        <Form form={surveyForm} layout="vertical" initialValues={{ score: 5 }}>
          <Form.Item name="score" label="评分（1～5）" rules={[{ required: true }]}>
            <Select
              options={[1, 2, 3, 4, 5].map((n) => ({ value: n, label: `${n} 分` }))}
            />
          </Form.Item>
          <Form.Item name="respondentName" label="评价人">
            <Input placeholder="可选" />
          </Form.Item>
          <Form.Item name="commentText" label="意见">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  );
}

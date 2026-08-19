import { DownloadOutlined, ReloadOutlined } from '@ant-design/icons';
import { ProCard } from '@ant-design/pro-components';
import {
  Alert,
  Button,
  Card,
  Empty,
  Modal,
  Progress,
  Select,
  Space,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { completeDefenseTask, exportDefenseSummary, getDefenseProgress } from '../../api/defenseScoring';
import { pagePreparationTasks } from '../../api/certificationPreparation';
import { PermissionGuard } from '../../directives/permission';
import type { PreparationTaskOverview } from '../../types/certificationPreparation';
import type { DefenseAssessorScoreView, DefenseCandidateProgress, DefenseOverallStatus } from '../../types/defenseScoring';
import { formatScore, statusLabel } from '../assessor/defenseScoreCatalog';
import { parseContentDispositionFilename } from '../../utils/download';

function overallLabel(status?: DefenseOverallStatus) {
  if (status === 'completed') return { color: 'success', text: '答辩已完成' };
  if (status === 'ready_to_complete') return { color: 'processing', text: '评分已齐，待完成答辩' };
  if (status === 'scoring') return { color: 'processing', text: '答辩评分中' };
  if (status === 'not_configured') return { color: 'default', text: '未配置综合答辩' };
  return { color: 'default', text: '待开始' };
}

function candidateStatusTag(row: DefenseCandidateProgress) {
  if (row.rejected) return <Tag color="error">不合格</Tag>;
  if (row.status === 'completed') return <Tag color="success">已完成</Tag>;
  if (row.status === 'scoring') return <Tag color="processing">评分中</Tag>;
  if (row.status === 'pending_report') return <Tag>待上传报告</Tag>;
  return <Tag>待评分</Tag>;
}

export function DefenseManagementPage() {
  const queryClient = useQueryClient();
  const [selectedTaskId, setSelectedTaskId] = useState<string>();
  const preferredTaskNo = useMemo(
    () => new URLSearchParams(window.location.search).get('taskNo') || '',
    [],
  );
  const tasksQuery = useQuery({
    queryKey: ['certification-defense-management-tasks'],
    queryFn: () => pagePreparationTasks({ page: 1, pageSize: 200, currentPhase: 'preparation' }),
  });
  const tasks = tasksQuery.data?.items || [];

  useEffect(() => {
    if (tasks.length === 0) return;
    const preferred = preferredTaskNo
      ? tasks.find((task) => task.taskNo === preferredTaskNo)
      : undefined;
    if (!selectedTaskId) {
      setSelectedTaskId(preferred?.id || tasks[0].id);
      return;
    }
    if (!tasks.some((task) => task.id === selectedTaskId)) {
      setSelectedTaskId(preferred?.id || tasks[0].id);
    }
  }, [preferredTaskNo, selectedTaskId, tasks]);

  const selectedTask = useMemo(
    () => tasks.find((task) => task.id === selectedTaskId),
    [selectedTaskId, tasks],
  );

  const progressQuery = useQuery({
    queryKey: ['defense-scoring-progress', selectedTaskId],
    queryFn: () => getDefenseProgress(selectedTaskId!),
    enabled: Boolean(selectedTaskId),
    refetchInterval: 5000,
  });

  const progress = progressQuery.data;
  const overall = overallLabel(progress?.overallStatus);
  const completeDisabled = progress?.overallStatus !== 'ready_to_complete';
  const exportDisabled = progress?.overallStatus !== 'ready_to_complete' && progress?.overallStatus !== 'completed';
  const completeDisabledReason = progress?.overallStatus === 'not_configured'
    ? '请先在认定筹备中绑定综合答辩考评组并上传业绩评估报告'
    : progress?.overallStatus === 'completed'
      ? '答辩已完成'
      : '全部考生的论文答辩和业绩评估提交后才能完成答辩';

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['certification-defense-management-tasks'] });
    if (selectedTaskId) {
      queryClient.invalidateQueries({ queryKey: ['defense-scoring-progress', selectedTaskId] });
    }
  };

  const completeMutation = useMutation({
    mutationFn: () => completeDefenseTask(selectedTaskId!),
    onSuccess: (result) => {
      message.success(`答辩已完成，共 ${result.candidateCount} 名考生`);
      refresh();
    },
  });

  const exportMutation = useMutation({
    mutationFn: () => exportDefenseSummary(selectedTaskId!),
    onSuccess: (resp) => {
      const filename = parseContentDispositionFilename({ headers: resp.headers as Record<string, string> })
        || `技师、高级技师评价综合评定科目评审成绩汇总表_${selectedTaskId}.docx`;
      const url = window.URL.createObjectURL(resp.data as Blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      message.success('成绩汇总表已生成');
    },
  });

  const taskLabel = (task: PreparationTaskOverview) =>
    `${task.taskNo} · ${task.taskName} · ${task.workTypeName || '-'} / ${task.levelName || '-'}`;

  const expertNames = progress?.experts || [];
  const expertCount = Math.max(3, expertNames.length);
  const expertIndexes = Array.from({ length: expertCount }, (_, index) => index);
  const columns: ColumnsType<DefenseCandidateProgress> = [
    { title: '准考证号', dataIndex: 'admissionTicketNo', width: 180, render: (v, row) => v || row.candidateNo || '-' },
    { title: '考生', dataIndex: 'candidateName', width: 110, fixed: 'left' },
    { title: '证件号', dataIndex: 'maskedIdCardNo', width: 170, render: (v) => v || '-' },
    ...expertIndexes.map((index) => ({
      title: `论文·${expertNames[index]?.assessorName || `专家${index + 1}`}`,
      width: 120,
      render: (_: unknown, row: DefenseCandidateProgress) => formatScore(row.thesisScores?.[index]),
    })),
    { title: '论文平均', dataIndex: 'thesisAverage', width: 100, render: (v) => formatScore(v) },
    ...expertIndexes.map((index) => ({
      title: `业绩·${expertNames[index]?.assessorName || `专家${index + 1}`}`,
      width: 120,
      render: (_: unknown, row: DefenseCandidateProgress) => formatScore(row.performanceScores?.[index]),
    })),
    { title: '业绩平均', dataIndex: 'performanceAverage', width: 100, render: (v) => formatScore(v) },
    {
      title: '最终成绩',
      width: 110,
      render: (_, row) => (row.rejected ? <Typography.Text type="danger">不合格</Typography.Text> : formatScore(row.finalScore)),
    },
    { title: '状态', width: 110, render: (_, row) => candidateStatusTag(row) },
  ];

  return (
    <PermissionGuard
      codes={['certification.defense.progress', 'certification.preparation.task.view']}
      mode="any"
      fallback={<Empty description="暂无答辩管理权限" />}
    >
      <div className="knowledge-page">
        <div className="page-heading knowledge-heading">
          <div>
            <Typography.Title level={1}>答辩管理</Typography.Title>
            <Typography.Text type="secondary">
              选择认定任务，查看综合答辩进度，并管理论文答辩与业绩评估评分数据
            </Typography.Text>
          </div>
          <Space>
            <Button icon={<ReloadOutlined />} onClick={refresh}>刷新</Button>
          </Space>
        </div>

        <Card title="选择认定任务" style={{ marginBottom: 16 }}>
          <Select
            showSearch
            optionFilterProp="label"
            loading={tasksQuery.isFetching}
            value={selectedTaskId}
            placeholder="请选择认定任务"
            style={{ width: 'min(720px, 100%)' }}
            options={tasks.map((task) => ({ value: task.id, label: taskLabel(task) }))}
            onChange={setSelectedTaskId}
          />
          {selectedTask ? (
            <Typography.Paragraph type="secondary" style={{ marginTop: 12, marginBottom: 0 }}>
              当前任务：{taskLabel(selectedTask)}
            </Typography.Paragraph>
          ) : null}
        </Card>

        {!selectedTaskId ? (
          <Empty description="请选择认定任务" />
        ) : (
          <ProCard
            title={(
              <Space>
                答辩评分进度
                <Tag color={overall.color}>{overall.text}</Tag>
                {progress?.groupName ? <Tag color="processing">{progress.groupName}</Tag> : null}
              </Space>
            )}
            extra={(
              <Space>
                <Tooltip title={completeDisabled ? completeDisabledReason : undefined}>
                  <span>
                    <Button
                      type="primary"
                      danger
                      disabled={completeDisabled}
                      loading={completeMutation.isPending}
                      onClick={() => {
                        Modal.confirm({
                          title: '确认完成答辩？',
                          content: '全部考生的论文答辩和业绩评估均已提交。完成后可导出技师、高级技师评价综合评定科目评审成绩汇总表。',
                          okText: '完成答辩',
                          cancelText: '取消',
                          onOk: () => completeMutation.mutateAsync(),
                        });
                      }}
                    >
                      完成答辩
                    </Button>
                  </span>
                </Tooltip>
                <Tooltip title={exportDisabled ? '全部考生完成答辩评分后才能导出' : undefined}>
                  <span>
                    <Button
                      icon={<DownloadOutlined />}
                      disabled={exportDisabled}
                      loading={exportMutation.isPending}
                      onClick={() => exportMutation.mutate()}
                    >
                      导出成绩汇总表
                    </Button>
                  </span>
                </Tooltip>
              </Space>
            )}
          >
            {progress?.overallStatus === 'not_configured' ? (
              <Alert
                type="info"
                showIcon
                style={{ marginBottom: 16 }}
                message="尚未配置综合答辩"
                description="请先到认定筹备「综合答辩配置」绑定考评组，并按考生上传业绩评估报告 PDF。考评员在综合答辩端完成论文答辩和业绩评估后，本页会汇总评分。"
              />
            ) : null}
            {progress?.overallStatus === 'ready_to_complete' ? (
              <Alert
                type="success"
                showIcon
                style={{ marginBottom: 16 }}
                message="全部考生答辩评分已提交"
                description="点击「完成答辩」后即可导出技师、高级技师评价综合评定科目评审成绩汇总表。"
              />
            ) : null}
            <Space size={24} style={{ marginBottom: 16 }}>
              <Progress
                type="circle"
                size={92}
                percent={progress?.totalCandidateCount
                  ? Math.round((progress.submittedCandidateCount || 0) * 100 / progress.totalCandidateCount)
                  : 0}
              />
              <Space direction="vertical" size={4}>
                <Typography.Text>
                  考生已完成 {progress?.submittedCandidateCount || 0} / {progress?.totalCandidateCount || 0} 人
                </Typography.Text>
                <Typography.Text type="secondary">
                  最终成绩 = 论文答辩平均分 × 60% + 业绩评估平均分 × 40%
                </Typography.Text>
                {progress?.examDate || progress?.examSite ? (
                  <Typography.Text type="secondary">
                    {[progress.examDate, progress.examSite].filter(Boolean).join(' · ')}
                  </Typography.Text>
                ) : null}
              </Space>
            </Space>
            <Table
              rowKey="candidateId"
              columns={columns}
              dataSource={progress?.candidates || []}
              loading={progressQuery.isFetching}
              pagination={false}
              scroll={{ x: 1600 }}
              expandable={{
                expandedRowRender: (row) => <AssessorBreakdown assessors={row.assessors} />,
                rowExpandable: (row) => (row.assessors || []).length > 0,
              }}
              locale={{ emptyText: '当前认定任务暂无考生' }}
            />
          </ProCard>
        )}
      </div>
    </PermissionGuard>
  );
}

function AssessorBreakdown({ assessors }: { assessors: DefenseAssessorScoreView[] }) {
  return (
    <Table
      size="small"
      rowKey="assessorId"
      pagination={false}
      dataSource={assessors}
      columns={[
        {
          title: '考评员',
          dataIndex: 'assessorName',
          render: (value: string, row: DefenseAssessorScoreView) => (
            <Space>
              {value || '-'}
              {row.leader ? <Tag color="gold">组长</Tag> : null}
            </Space>
          ),
        },
        {
          title: '论文答辩',
          render: (_, row) => (
            <Space>
              <Tag color={row.thesisStatus === 'submitted' ? 'green' : row.thesisStatus === 'draft' ? 'gold' : 'default'}>
                {statusLabel(row.thesisStatus)}
              </Tag>
              <span>{formatScore(row.thesisScore)}</span>
              {row.thesisRejected ? <Tag color="error">否定</Tag> : null}
            </Space>
          ),
        },
        {
          title: '业绩评估',
          render: (_, row) => (
            <Space>
              <Tag color={row.performanceStatus === 'submitted' ? 'green' : row.performanceStatus === 'draft' ? 'gold' : 'default'}>
                {statusLabel(row.performanceStatus)}
              </Tag>
              <span>{formatScore(row.performanceScore)}</span>
              {row.performanceRejected ? <Tag color="error">否定</Tag> : null}
            </Space>
          ),
        },
      ]}
    />
  );
}

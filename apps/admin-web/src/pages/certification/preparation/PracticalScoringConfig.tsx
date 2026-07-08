import {
  DeleteOutlined,
  DownloadOutlined,
  LinkOutlined,
  ReloadOutlined,
  RollbackOutlined,
  UploadOutlined,
} from '@ant-design/icons';
import { ProCard } from '@ant-design/pro-components';
import {
  Alert,
  Button,
  Empty,
  Input,
  InputNumber,
  Modal,
  Progress,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  Upload,
  message,
} from 'antd';
import './practical-scoring-expand.css';
import type { ColumnsType } from 'antd/es/table';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import {
  listAllPreparationCandidates,
  listPreparationAssessorGroups,
} from '../../../api/certificationPreparation';
import {
  adjustPracticalScoringTaskScore,
  cancelPracticalCandidateGeneratedTasks,
  exportPracticalSummary,
  getPracticalScoringTaskDetail,
  getPracticalScoringProgress,
  getPracticalScoringSetup,
  importPracticalPaper,
  linkPracticalCandidateTemplate,
  markPracticalCandidateAbsent,
  returnPracticalScoringTask,
} from '../../../api/practicalScoring';
import { parseContentDispositionFilename } from '../../../utils/download';
import type {
  PreparationAssessorGroup,
  PreparationCandidate,
} from '../../../types/certificationPreparation';
import type {
  PracticalCandidateProgress,
  PracticalPaperOption,
  PracticalScoreItem,
  PracticalScopeOption,
  PracticalTaskProgressItem,
} from '../../../types/practicalScoring';

interface Props {
  taskId?: string;
  showSetup?: boolean;
  showTemplate?: boolean;
  showProgress?: boolean;
}

interface CandidateLinkValues {
  candidateId: string;
  scopeId?: string;
  paperId: string;
  groupId: string;
  workTypeId?: number | null;
}

export function PracticalScoringConfig({
  taskId,
  showSetup = true,
  showTemplate = true,
  showProgress = true,
}: Props) {
  const queryClient = useQueryClient();
  const [selectedGroupId, setSelectedGroupId] = useState<string>();
  const [selectedScopeId, setSelectedScopeId] = useState<string>();
  const [selectedPaperByCandidate, setSelectedPaperByCandidate] = useState<Record<string, string | undefined>>({});
  const [rowErrors, setRowErrors] = useState<Record<string, string | undefined>>({});
  const [returningTask, setReturningTask] = useState<PracticalTaskProgressItem>();
  const [returnReason, setReturnReason] = useState('');
  const [adjustingTask, setAdjustingTask] = useState<PracticalTaskProgressItem>();
  const [adjustDeductions, setAdjustDeductions] = useState<Record<string, number>>({});
  const [adjustRemarks, setAdjustRemarks] = useState<Record<string, string>>({});
  const [adjustNote, setAdjustNote] = useState('');
  const [adjustReason, setAdjustReason] = useState('');
  const [selectedSummaryCandidateIds, setSelectedSummaryCandidateIds] = useState<string[]>([]);
  const showConfig = showSetup || showTemplate;

  const setupQuery = useQuery({
    queryKey: ['practical-scoring-setup', taskId],
    queryFn: () => getPracticalScoringSetup(taskId!),
    enabled: Boolean(taskId && showConfig),
  });
  const progressQuery = useQuery({
    queryKey: ['practical-scoring-progress', taskId],
    queryFn: () => getPracticalScoringProgress(taskId!),
    enabled: Boolean(taskId && (showConfig || showProgress)),
    refetchInterval: showProgress ? 5000 : false,
  });
  const adjustingTaskQuery = useQuery({
    queryKey: ['practical-scoring-admin-task', adjustingTask?.assessorTaskId],
    queryFn: () => getPracticalScoringTaskDetail(adjustingTask!.assessorTaskId),
    enabled: Boolean(adjustingTask),
  });
  const groupsQuery = useQuery({
    queryKey: ['certification-preparation-assessor-groups', taskId],
    queryFn: () => listPreparationAssessorGroups(taskId!),
    enabled: Boolean(taskId && showConfig),
  });

  const setup = setupQuery.data;
  const selectedScope = useMemo(
    () => setup?.scopes.find((scope) => scope.id === selectedScopeId),
    [selectedScopeId, setup?.scopes],
  );
  const candidateQuery = useQuery({
    queryKey: ['practical-scoring-candidates', taskId, selectedScopeId, selectedScope?.levelCode],
    queryFn: () => listAllPreparationCandidates(taskId!, {
      status: 'active',
      documentComplete: true,
      ...(selectedScope?.levelCode ? { levelCode: selectedScope.levelCode } : {}),
    }),
    enabled: Boolean(taskId && showConfig),
  });

  const groupDetailById = useMemo(
    () => new Map((groupsQuery.data || []).map((group) => [String(group.id), group])),
    [groupsQuery.data],
  );
  const resolveMemberCount = (groupId: string) => {
    const detail = groupDetailById.get(String(groupId));
    if (typeof detail?.memberCount === 'number') {
      return detail.memberCount;
    }
    const setupGroup = setup?.groups.find((group) => String(group.id) === String(groupId));
    return setupGroup?.memberCount ?? 0;
  };
  const configByCandidate = useMemo(
    () => new Map((setup?.configs || [])
      .filter((config) => config.candidateId)
      .map((config) => [config.candidateId!, config])),
    [setup?.configs],
  );
  const progressByCandidate = useMemo(
    () => new Map((progressQuery.data?.candidates || []).map((candidate) => [candidate.candidateId, candidate])),
    [progressQuery.data?.candidates],
  );
  const abnormalCandidates = useMemo(
    () => (progressQuery.data?.candidates || []).filter((candidate) => candidate.status === 'review_required'),
    [progressQuery.data?.candidates],
  );
  const submittedCandidateCount = useMemo(
    () => (progressQuery.data?.candidates || []).filter((candidate) => candidate.status === 'completed').length,
    [progressQuery.data?.candidates],
  );
  const totalCandidateCount = progressQuery.data?.candidates.length || 0;
  const assessorGroupsByCandidate = useMemo(() => {
    const groups = new Map<string, Map<string, { id: string; name: string; tasks: PracticalTaskProgressItem[] }>>();
    for (const task of progressQuery.data?.scoringTasks || []) {
      let byCandidate = groups.get(task.candidateId);
      if (!byCandidate) {
        byCandidate = new Map();
        groups.set(task.candidateId, byCandidate);
      }
      const byAssessor = byCandidate.get(task.assessorId);
      if (byAssessor) {
        byAssessor.tasks.push(task);
      } else {
        byCandidate.set(task.assessorId, { id: task.assessorId, name: task.assessorName, tasks: [task] });
      }
    }
    return groups;
  }, [progressQuery.data?.scoringTasks]);
  const candidateRows = useMemo(() => {
    const rows = candidateQuery.data?.items || [];
    if (!selectedScope) return rows;
    return rows.filter((candidate) => scopeMatchesCandidate(selectedScope, candidate));
  }, [candidateQuery.data?.items, selectedScope]);

  const refresh = () => {
    if (showConfig) {
      queryClient.invalidateQueries({ queryKey: ['practical-scoring-setup', taskId] });
      queryClient.invalidateQueries({ queryKey: ['practical-scoring-candidates', taskId] });
      queryClient.invalidateQueries({ queryKey: ['certification-preparation-assessor-groups', taskId] });
    }
    if (showProgress || showConfig) {
      queryClient.invalidateQueries({ queryKey: ['practical-scoring-progress', taskId] });
    }
  };

  useEffect(() => {
    const detail = adjustingTaskQuery.data;
    if (!detail) return;
    setAdjustDeductions(Object.fromEntries(
      detail.scoreItems.map((item) => [scoreItemKey(item), item.deductedScore || 0]),
    ));
    setAdjustRemarks(Object.fromEntries(
      detail.scoreItems.map((item) => [scoreItemKey(item), item.remark || '']),
    ));
    setAdjustNote(detail.progressNote || '');
  }, [adjustingTaskQuery.data]);

  const adjustTotalScore = useMemo(() => {
    const detail = adjustingTaskQuery.data;
    if (!detail) return 0;
    return detail.scoreItems.reduce((sum, item) => (
      sum + Math.max(0, item.maxScore - (adjustDeductions[scoreItemKey(item)] ?? item.deductedScore ?? 0))
    ), 0);
  }, [adjustDeductions, adjustingTaskQuery.data]);

  const candidateLinkMutation = useMutation({
    mutationFn: (values: CandidateLinkValues) =>
      linkPracticalCandidateTemplate(taskId!, values.candidateId, {
        scopeId: values.scopeId,
        paperId: values.paperId,
        groupId: values.groupId,
        workTypeId: values.workTypeId,
      }),
    onSuccess: (result, values) => {
      message.success(`已生成 ${result.createdTasks} 个评分任务，已存在 ${result.existingTasks} 个`);
      setRowErrors((prev) => ({ ...prev, [values.candidateId]: undefined }));
      refresh();
    },
    onError: (error: unknown, values) => {
      const reason =
        (error as { response?: { data?: { message?: string } }; message?: string })?.response?.data?.message
        || (error as { message?: string })?.message
        || '配置实操试卷失败，请稍后重试';
      setRowErrors((prev) => ({ ...prev, [values.candidateId]: reason }));
      message.error(reason);
    },
  });
  const cancelCandidateTasksMutation = useMutation({
    mutationFn: (candidateId: string) => cancelPracticalCandidateGeneratedTasks(taskId!, candidateId),
    onSuccess: () => {
      message.success('评分任务已取消，可重新编辑试卷和考评组');
      refresh();
    },
    onError: (error: unknown) => {
      const reason =
        (error as { response?: { data?: { message?: string } }; message?: string })?.response?.data?.message
        || (error as { message?: string })?.message
        || '取消评分任务失败，请稍后重试';
      message.error(reason);
    },
  });
  const returnMutation = useMutation({
    mutationFn: () => returnPracticalScoringTask(returningTask!.assessorTaskId, returnReason),
    onSuccess: () => {
      message.success('评分任务已退回原考评员');
      setReturningTask(undefined);
      setReturnReason('');
      refresh();
    },
  });
  const adjustMutation = useMutation({
    mutationFn: () => {
      const detail = adjustingTaskQuery.data!;
      return adjustPracticalScoringTaskScore(detail.task.id, {
        version: detail.task.version,
        reason: adjustReason,
        note: adjustNote || undefined,
        items: detail.scoreItems.map((item) => ({
          scoreItemId: item.id,
          itemNo: item.itemNo,
          deductedScore: adjustDeductions[scoreItemKey(item)] ?? item.deductedScore ?? 0,
          remark: adjustRemarks[scoreItemKey(item)] || undefined,
        })),
      });
    },
    onSuccess: () => {
      message.success('评分已修改并重新提交');
      setAdjustingTask(undefined);
      setAdjustReason('');
      setAdjustNote('');
      setAdjustDeductions({});
      setAdjustRemarks({});
      refresh();
    },
  });
  const candidateAbsentMutation = useMutation({
    mutationFn: (candidateId: string) => markPracticalCandidateAbsent(taskId!, candidateId),
    onSuccess: () => {
      message.success('已标记缺考');
      refresh();
    },
  });
  const importPaperMutation = useMutation({
    mutationFn: (file: File) => importPracticalPaper(taskId!, file),
    onSuccess: (paper) => {
      message.success(`已导入 ${paper.title}，生成 ${paper.questions} 道实操题、${paper.scoreItems} 个评分项`);
      refresh();
    },
    onError: (error: unknown) => {
      const reason =
        (error as { response?: { data?: { message?: string } }; message?: string })?.response?.data?.message
        || (error as { message?: string })?.message
        || '导入评分记录表失败，请检查表格格式';
      message.error(reason);
    },
  });
  const exportSummaryMutation = useMutation({
    mutationFn: () => exportPracticalSummary(taskId!, selectedSummaryCandidateIds),
    onSuccess: (resp) => {
      const filename = parseContentDispositionFilename({ headers: resp.headers as Record<string, string> })
        || `评分记录汇总表_${taskId}.xlsx`;
      const url = window.URL.createObjectURL(resp.data as Blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      message.success('实操评分表已生成');
    },
  });

  if (!taskId) return <Typography.Text type="secondary">请选择认定任务。</Typography.Text>;

  const configureCandidate = (candidate: PreparationCandidate) => {
    const paperId = selectedPaperByCandidate[candidate.id] || configByCandidate.get(candidate.id)?.paperId;
    const scope = selectedScope && scopeMatchesCandidate(selectedScope, candidate)
      ? selectedScope
      : findScopeForCandidate(setup?.scopes || [], candidate);
    if (!selectedGroupId) {
      message.warning('请选择三人考评组');
      return;
    }
    if (!paperId) {
      message.warning('请选择实操试卷');
      return;
    }
    if (!scope) {
      message.warning('未找到匹配学员工种等级的任务范围');
      return;
    }
    const paper = setup?.papers.find((item) => item.id === paperId);
    candidateLinkMutation.mutate({
      candidateId: candidate.id,
      scopeId: scope.id,
      paperId,
      groupId: selectedGroupId,
      workTypeId: toNullableNumber(candidate.workTypeId || scope.workTypeId || paper?.workTypeId),
    });
  };

  const groupColumns: ColumnsType<PreparationAssessorGroup> = [
    { title: '考评组', dataIndex: 'groupName', ellipsis: true },
    { title: '组长', dataIndex: 'leaderAssessorName', width: 120, render: (value) => value || '-' },
    {
      title: '负责工种等级',
      dataIndex: 'workTypeScopes',
      render: (scopes: PreparationAssessorGroup['workTypeScopes'], row) =>
        scopes?.length
          ? scopes.map((scope) => `${scope.workTypeName || '-'} / ${scope.levelName || scope.levelCode || '-'}`).join('、')
          : row.workTypeName || '-',
    },
    {
      title: '成员',
      width: 100,
      render: (_, row) => {
        const count = resolveMemberCount(row.id);
        return <Tag color={count === 3 ? 'success' : 'warning'}>{count}/3</Tag>;
      },
    },
    {
      title: '选择',
      width: 100,
      render: (_, row) => {
        const count = resolveMemberCount(row.id);
        return (
          <Button
            type={selectedGroupId === row.id ? 'primary' : 'default'}
            size="small"
            disabled={count !== 3}
            onClick={() => setSelectedGroupId(row.id)}
          >
            选择
          </Button>
        );
      },
    },
  ];

  const candidateColumns: ColumnsType<PreparationCandidate> = [
    { title: '学员', dataIndex: 'candidateName', width: 120, fixed: 'left' },
    { title: '证件号', dataIndex: 'maskedIdCardNo', width: 180, render: (v, row) => v || row.idCardNo || '-' },
    { title: '手机号', dataIndex: 'maskedPhone', width: 140, render: (v, row) => v || row.phone || '-' },
    { title: '工种', dataIndex: 'workTypeName', width: 120, render: (v) => v || '-' },
    { title: '等级', dataIndex: 'levelName', width: 100, render: (v, row) => v || row.levelCode || '-' },
    { title: '单位', dataIndex: 'orgName', width: 160, ellipsis: true, render: (v) => v || '-' },
    {
      title: '实操试卷',
      width: 260,
      render: (_, row) => {
        const configured = configByCandidate.get(row.id);
        const rowScope = selectedScope && scopeMatchesCandidate(selectedScope, row)
          ? selectedScope
          : findScopeForCandidate(setup?.scopes || [], row);
        return (
          <Select
            showSearch
            optionFilterProp="label"
            placeholder="请选择试卷"
            value={selectedPaperByCandidate[row.id] || configured?.paperId}
            style={{ width: '100%' }}
            disabled={Boolean(progressByCandidate.get(row.id)?.totalTasks)}
            options={paperOptionsForCandidate(setup?.papers || [], row, rowScope).map((paper) => ({
              value: paper.id,
              label: paper.title,
              disabled: paper.questions.length === 0 || paper.questions.some((question) => !question.templateReady),
            }))}
            onChange={(paperId) => setSelectedPaperByCandidate((prev) => ({ ...prev, [row.id]: paperId }))}
          />
        );
      },
    },
    {
      title: '已绑定考评组',
      width: 180,
      render: (_, row) => {
        const config = configByCandidate.get(row.id);
        if (!config) return '-';
        return groupDetailById.get(String(config.groupId))?.groupName
          || setup?.groups.find((group) => String(group.id) === String(config.groupId))?.groupName
          || '-';
      },
    },
    {
      title: '配置状态',
      width: 180,
      render: (_, row) => {
        const error = rowErrors[row.id];
        const configured = configByCandidate.get(row.id);
        const progress = progressByCandidate.get(row.id);
        if (error) return <Tag color="error">配置失败</Tag>;
        if (progress?.totalTasks) return <Tag color="success">已生成评分任务</Tag>;
        if (configured) return <Tag color="processing">已配置</Tag>;
        return <Tag>未配置</Tag>;
      },
    },
    {
      title: '操作',
      width: 130,
      fixed: 'right',
      render: (_, row) => {
        const hasGeneratedTasks = Boolean(progressByCandidate.get(row.id)?.totalTasks);
        if (hasGeneratedTasks) {
          return (
            <Button
              danger
              type="link"
              icon={<DeleteOutlined />}
              loading={cancelCandidateTasksMutation.isPending}
              onClick={() => Modal.confirm({
                title: '取消已生成的评分任务？',
                content: '仅未开考的考生可以取消。评分任务、评分明细和汇总结果将被删除，随后可重新编辑试卷和考评组。',
                okText: '确认取消',
                okButtonProps: { danger: true },
                cancelText: '暂不取消',
                onOk: () => cancelCandidateTasksMutation.mutateAsync(row.id),
              })}
            >
              取消
            </Button>
          );
        }
        return (
          <Button
            type="link"
            icon={<LinkOutlined />}
            loading={candidateLinkMutation.isPending}
            onClick={() => configureCandidate(row)}
          >
            配置试卷
          </Button>
        );
      },
    },
  ];

  const progressCandidateColumns: ColumnsType<PracticalCandidateProgress> = [
    { title: '考生编号', dataIndex: 'candidateNo', width: 120, render: (v) => v || '-' },
    { title: '考生', dataIndex: 'candidateName', width: 120 },
    {
      title: '评分提交',
      width: 180,
      render: (_, row) => (
        <Progress
          percent={row.totalTasks ? Math.round(row.submittedTasks * 100 / row.totalTasks) : 0}
          format={() => `${row.submittedTasks}/${row.totalTasks}`}
        />
      ),
    },
    { title: '试题完成', width: 150, render: (_, row) => `${row.completedQuestions}/${row.totalQuestions}` },
    { title: '实操总分', dataIndex: 'totalScore', width: 110, render: (v) => formatScore(v) ?? '待三人齐交' },
    {
      title: '开考协同',
      width: 220,
      render: (_, row) => {
        const sessionStatus = row.startSession?.status;
        const absentDisabled =
          row.status === 'completed' || row.status === 'absent' || sessionStatus === 'completed';
        return (
          <Space>
            {row.startSession ? (
              <Tag color={sessionStatus === 'completed' ? 'success' : sessionStatus === 'absent' ? 'warning' : sessionStatus === 'started' ? 'processing' : 'default'}>
                {sessionStatus === 'completed'
                  ? '考试已完成'
                  : sessionStatus === 'absent'
                    ? '缺考'
                    : sessionStatus === 'started'
                      ? '考试中'
                      : `${row.startSession.candidateConfirmed ? '已选择考生' : '待确认'} · ${row.startSession.confirmedAssessors}/3`}
              </Tag>
            ) : <Tag>待生成任务</Tag>}
            <Button
              size="small"
              danger
              disabled={absentDisabled}
              loading={candidateAbsentMutation.isPending}
              onClick={(event) => {
                event.stopPropagation();
                Modal.confirm({
                  title: '确认标记缺考？',
                  content: `${row.candidateName} 的实操评分数据将按 0.0 导出。`,
                  okText: '确认缺考',
                  cancelText: '取消',
                  okButtonProps: { danger: true },
                  onOk: () => candidateAbsentMutation.mutateAsync(row.candidateId),
                });
              }}
            >
              缺考
            </Button>
          </Space>
        );
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (v) => {
        if (v === 'review_required') return <Tag color="error">分差异常</Tag>;
        if (v === 'absent') return <Tag color="warning">缺考</Tag>;
        return <Tag color={v === 'completed' ? 'success' : 'processing'}>{v === 'completed' ? '已完成' : '评分中'}</Tag>;
      },
    },
  ];

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      {showConfig && (
        <ProCard
          title="区域一：考评组配置区"
          extra={<Button icon={<ReloadOutlined />} onClick={refresh}>刷新</Button>}
        >
          <Space direction="vertical" size={12} style={{ width: '100%' }}>
            <Select
              showSearch
              optionFilterProp="label"
              value={selectedGroupId}
              placeholder="请选择满足三人成组条件的考评组"
              style={{ width: 'min(520px, 100%)' }}
              options={(groupsQuery.data || []).map((group) => {
                const count = resolveMemberCount(group.id);
                return {
                  value: group.id,
                  disabled: count !== 3,
                  label: `${group.groupName}（${count}/3人${group.leaderAssessorName ? ` · 组长${group.leaderAssessorName}` : ''}）`,
                };
              })}
              onChange={setSelectedGroupId}
            />
            <Table
              rowKey="id"
              size="small"
              loading={groupsQuery.isFetching || setupQuery.isFetching}
              dataSource={groupsQuery.data || []}
              columns={groupColumns}
              pagination={false}
              locale={{ emptyText: <Empty description="暂无考评组" /> }}
            />
          </Space>
        </ProCard>
      )}

      {showConfig && (
        <ProCard title="区域二：学员列表区">
          <Space direction="vertical" size={12} style={{ width: '100%' }}>
            <Space wrap>
              <Select
                allowClear
                showSearch
                optionFilterProp="label"
                value={selectedScopeId}
                placeholder="工种等级筛选"
                style={{ width: 260 }}
                options={(setup?.scopes || []).map((scope) => ({
                  value: scope.id,
                  label: `${scope.workTypeName || '-'} / ${scope.levelName || scope.levelCode || '-'}`,
                }))}
                onChange={setSelectedScopeId}
              />
              <Upload
                accept=".xlsx,.xls"
                showUploadList={false}
                beforeUpload={(file) => {
                  importPaperMutation.mutate(file);
                  return false;
                }}
              >
                <Button icon={<UploadOutlined />} loading={importPaperMutation.isPending}>
                  导入评分记录表
                </Button>
              </Upload>
              {selectedGroupId ? (
                <Tag color="processing">
                  已选考评组：{groupDetailById.get(String(selectedGroupId))?.groupName
                    || setup?.groups.find((group) => String(group.id) === String(selectedGroupId))?.groupName
                    || selectedGroupId}
                </Tag>
              ) : null}
            </Space>
            <Table
              rowKey="id"
              columns={candidateColumns}
              dataSource={candidateRows}
              loading={candidateQuery.isFetching || setupQuery.isFetching || progressQuery.isFetching}
              scroll={{ x: 1490 }}
              expandable={{
                expandedRowRender: (row) => rowErrors[row.id]
                  ? <Typography.Text type="danger">{rowErrors[row.id]}</Typography.Text>
                  : null,
                rowExpandable: (row) => Boolean(rowErrors[row.id]),
              }}
              pagination={{ pageSize: 10 }}
              locale={{
                emptyText: selectedScopeId ? '当前工种等级下暂无已完成申报材料的学员' : '暂无已完成申报材料的学员',
              }}
            />
          </Space>
        </ProCard>
      )}

      {showProgress && (
        <ProCard title="评分进度">
          <Space wrap style={{ marginBottom: 16 }}>
            <Button
              icon={<DownloadOutlined />}
              disabled={selectedSummaryCandidateIds.length === 0}
              loading={exportSummaryMutation.isPending}
              onClick={() => exportSummaryMutation.mutate()}
            >
              导出实操评分表
            </Button>
          </Space>
          {abnormalCandidates.length > 0 && (
            <Alert
              type="error"
              showIcon
              style={{ marginBottom: 16 }}
              message={`发现 ${abnormalCandidates.length} 名考生成绩分差异常`}
              description="请展开考生行查看标记为分差异常的试题，在对应考评员任务上点击修改分数并重新提交。偏差不超过10分后，组长端才能完成考试。"
            />
          )}
          <Space size={24} style={{ marginBottom: 16 }}>
            <Progress
              type="circle"
              size={92}
              style={{ marginLeft: 7 }}
              percent={totalCandidateCount
                ? Math.round(submittedCandidateCount * 100 / totalCandidateCount)
                : 0}
            />
            <Typography.Text>
              考生已提交 {submittedCandidateCount} / {totalCandidateCount} 人
            </Typography.Text>
          </Space>
          <Table
            rowKey="candidateId"
            columns={progressCandidateColumns}
            dataSource={progressQuery.data?.candidates || []}
            pagination={false}
            scroll={{ x: 850 }}
            rowSelection={{
              selectedRowKeys: selectedSummaryCandidateIds,
              onChange: (keys) => setSelectedSummaryCandidateIds(keys.map(String)),
              getCheckboxProps: (row) => ({
                disabled: row.status !== 'completed' && row.status !== 'absent',
              }),
            }}
            expandable={{
              expandedRowRender: (row) => (
                <AssessorBreakdown
                  assessorGroups={assessorGroupsByCandidate.get(row.candidateId)}
                  onReturnTask={setReturningTask}
                  onAdjustTask={(task) => {
                    setAdjustReason('');
                    setAdjustingTask(task);
                  }}
                />
              ),
              rowExpandable: (row) => (assessorGroupsByCandidate.get(row.candidateId)?.size ?? 0) > 0,
              expandRowByClick: true,
            }}
          />
        </ProCard>
      )}

      <Modal
        title="退回评分任务"
        open={Boolean(returningTask)}
        okButtonProps={{ danger: true, disabled: !returnReason.trim() }}
        confirmLoading={returnMutation.isPending}
        onCancel={() => setReturningTask(undefined)}
        onOk={() => returnMutation.mutate()}
      >
        <Typography.Paragraph>
          {returningTask?.candidateName} / {returningTask?.questionTitle} / {returningTask?.assessorName}
        </Typography.Paragraph>
        <Input.TextArea
          rows={4}
          value={returnReason}
          onChange={(event) => setReturnReason(event.target.value)}
          placeholder="必须填写退回原因，原成绩将立即失效"
        />
      </Modal>

      <Modal
        title="修改异常评分"
        open={Boolean(adjustingTask)}
        width={760}
        okText="保存并重新提交"
        confirmLoading={adjustMutation.isPending}
        okButtonProps={{
          disabled: !adjustingTaskQuery.data || !adjustReason.trim(),
        }}
        onCancel={() => {
          setAdjustingTask(undefined);
          setAdjustReason('');
        }}
        onOk={() => adjustMutation.mutate()}
      >
        <Space direction="vertical" size={12} style={{ width: '100%' }}>
          <Typography.Paragraph>
            {adjustingTask?.candidateName} / {adjustingTask?.questionTitle} / {adjustingTask?.assessorName}
          </Typography.Paragraph>
          {adjustingTask?.reviewRequired ? (
            <Alert
              type="error"
              showIcon
              message={`当前试题三人评分分差${adjustingTask.scoreDeviation ?? '-'}分，超过10分`}
            />
          ) : null}
          <Table<PracticalScoreItem>
            rowKey={scoreItemKey}
            loading={adjustingTaskQuery.isFetching}
            dataSource={adjustingTaskQuery.data?.scoreItems || []}
            pagination={false}
            columns={[
              { title: '序号', dataIndex: 'itemNo', width: 70 },
              { title: '评分项', render: (_, item) => scoreStandardContent(item) },
              { title: '满分', dataIndex: 'maxScore', width: 80 },
              {
                title: '扣分',
                width: 150,
                render: (_, item) => {
                  const key = scoreItemKey(item);
                  return (
                    <InputNumber
                      min={0}
                      max={item.maxDeduction}
                      step={item.deductionStep}
                      value={adjustDeductions[key] ?? item.deductedScore ?? 0}
                      onChange={(value) => setAdjustDeductions((prev) => ({
                        ...prev,
                        [key]: Number(value || 0),
                      }))}
                    />
                  );
                },
              },
              {
                title: '得分',
                width: 90,
                render: (_, item) => Math.max(0, item.maxScore - (adjustDeductions[scoreItemKey(item)] ?? item.deductedScore ?? 0)),
              },
              {
                title: '说明',
                width: 220,
                render: (_, item) => {
                  const key = scoreItemKey(item);
                  return (
                    <Input
                      value={adjustRemarks[key] || ''}
                      onChange={(event) => setAdjustRemarks((prev) => ({
                        ...prev,
                        [key]: event.target.value,
                      }))}
                    />
                  );
                },
              },
            ]}
          />
          <Typography.Text strong>调整后本题得分：{adjustTotalScore}</Typography.Text>
          <Input.TextArea
            rows={2}
            value={adjustNote}
            onChange={(event) => setAdjustNote(event.target.value)}
            placeholder="考评说明（选填）"
          />
          <Input.TextArea
            rows={3}
            value={adjustReason}
            onChange={(event) => setAdjustReason(event.target.value)}
            placeholder="必须填写修改原因，用于审计留痕"
          />
        </Space>
      </Modal>
    </Space>
  );
}

interface AssessorBreakdownProps {
  assessorGroups?: Map<string, { id: string; name: string; tasks: PracticalTaskProgressItem[] }>;
  onReturnTask?: (task: PracticalTaskProgressItem) => void;
  onAdjustTask?: (task: PracticalTaskProgressItem) => void;
}

function AssessorBreakdown({ assessorGroups, onReturnTask, onAdjustTask }: AssessorBreakdownProps) {
  const slots = useMemo(() => {
    if (!assessorGroups) return [];
    return Array.from(assessorGroups.values()).sort((a, b) => a.id.localeCompare(b.id));
  }, [assessorGroups]);

  if (slots.length === 0) {
    return <Typography.Text type="secondary">暂无考评员评分任务</Typography.Text>;
  }

  // 补齐到 3 列占位，保证三栏对齐
  const padded: Array<typeof slots[number] | null> = [...slots];
  while (padded.length < 3) padded.push(null);

  return (
    <div className="practical-candidate-assessors">
      {padded.map((assessor, index) => {
        if (!assessor) {
          return (
            <div
              key={`empty-${index}`}
              className="practical-candidate-assessor practical-candidate-assessor--empty"
            >
              该位置暂无考评员
            </div>
          );
        }
        const submitted = assessor.tasks.filter((task) => task.status === 'submitted').length;
        const total = assessor.tasks.length;
        const totalScore = assessor.tasks.reduce((sum, task) => sum + (task.totalScore || 0), 0);
        const allSubmitted = total > 0 && submitted === total;
        const anyReturned = assessor.tasks.some((task) => task.status === 'returned');
        let statusColor: string = 'processing';
        let statusText = '评分中';
        if (allSubmitted) {
          statusColor = 'success';
          statusText = '已提交';
        } else if (anyReturned) {
          statusColor = 'error';
          statusText = '已退回';
        }
        return (
          <div key={assessor.id} className="practical-candidate-assessor">
            <div className="practical-assessor-head">
              <span className="assessor-name">{assessor.name}</span>
              <Tag color={statusColor}>{statusText}</Tag>
            </div>
            <div className="practical-assessor-progress">
              <Progress
                percent={total ? Math.round((submitted * 100) / total) : 0}
                size="small"
                style={{ flex: 1, margin: 0 }}
              />
              <span>{submitted}/{total} 已提交</span>
            </div>
            <div className="practical-assessor-tasks">
              {assessor.tasks.map((task) => {
                const taskColor = task.status === 'submitted'
                  ? 'success'
                  : task.status === 'returned'
                    ? 'error'
                    : 'processing';
                const canReturn = task.status === 'submitted';
                const reviewRequired = Boolean(task.reviewRequired);
                return (
                  <div
                    key={task.assessorTaskId}
                    className={`practical-assessor-task${reviewRequired ? ' is-review-required' : ''}`}
                  >
                    <span className="task-title">{task.questionTitle}</span>
                    <span className="task-score">{formatScore(task.totalScore) ?? '-'}</span>
                    <Tag color={taskColor}>{statusName(task.status)}</Tag>
                    {reviewRequired ? (
                      <Tag color="error">分差异常{task.scoreDeviation != null ? ` ${task.scoreDeviation}分` : ''}</Tag>
                    ) : null}
                    {onAdjustTask ? (
                      <Button
                        type="link"
                        size="small"
                        disabled={!canReturn}
                        onClick={(event) => {
                          event.stopPropagation();
                          onAdjustTask(task);
                        }}
                      >
                        修改分数
                      </Button>
                    ) : null}
                    {onReturnTask ? (
                      <Button
                        type="link"
                        size="small"
                        danger
                        disabled={!canReturn}
                        icon={<RollbackOutlined />}
                        onClick={(event) => {
                          event.stopPropagation();
                          onReturnTask(task);
                        }}
                      >
                        退回
                      </Button>
                    ) : null}
                  </div>
                );
              })}
            </div>
            <div className="practical-assessor-total">
              <span>合计分</span>
              <strong>{formatScore(totalScore)}</strong>
            </div>
          </div>
        );
      })}
    </div>
  );
}

function scopeMatchesCandidate(scope: PracticalScopeOption, candidate: PreparationCandidate) {
  const sameLevel = !scope.levelCode || scope.levelCode === candidate.levelCode;
  const sameWorkType = candidate.workTypeId
    ? scope.workTypeId === candidate.workTypeId || scope.workTypeName === candidate.workTypeName
    : scope.workTypeName === candidate.workTypeName;
  return sameLevel && sameWorkType;
}

function findScopeForCandidate(scopes: PracticalScopeOption[], candidate: PreparationCandidate) {
  return scopes.find((scope) => scopeMatchesCandidate(scope, candidate));
}

function paperOptionsForCandidate(
  papers: PracticalPaperOption[],
  candidate: PreparationCandidate,
  scope?: PracticalScopeOption,
) {
  return papers.filter((paper) => {
    const workTypeId = candidate.workTypeId || scope?.workTypeId;
    const sameWorkType = !paper.workTypeId || paper.workTypeId === workTypeId;
    const sameLevel = !paper.level || paper.level === candidate.levelCode || paper.level === candidate.levelName;
    return sameWorkType && sameLevel;
  });
}

function toNullableNumber(value?: string | number | null) {
  if (value === undefined || value === null || value === '') return null;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

function scoreItemKey(item: PracticalScoreItem) {
  return item.id || String(item.itemNo);
}

function scoreStandardContent(item: PracticalScoreItem) {
  return item.scoringStandardContent || item.criterion || item.itemName || '-';
}

function formatScore(value?: number | null) {
  if (value === undefined || value === null) return undefined;
  const numeric = Number(value);
  return Number.isFinite(numeric) ? numeric.toFixed(1) : undefined;
}

function statusName(status: string) {
  return { assigned: '待评分', draft: '草稿', submitted: '已提交', returned: '已退回', absent: '缺考' }[status] || status;
}

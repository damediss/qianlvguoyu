import {
  DeleteOutlined,
  DownloadOutlined,
  EditOutlined,
  PlusOutlined,
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
  Tooltip,
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
  batchLinkPracticalCandidateTemplates,
  completePracticalScoringTask,
  requestPracticalCompleteSign,
  adjustPracticalScoringTaskScore,
  exportPracticalSummary,
  getPracticalScoringTaskDetail,
  getPracticalScoringProgress,
  getPracticalScoringSetup,
  importPracticalPaper,
  markPracticalCandidateAbsent,
  returnPracticalScoringTask,
  savePracticalQuestion,
} from '../../../api/practicalScoring';
import { parseContentDispositionFilename } from '../../../utils/download';
import type {
  PreparationCandidate,
} from '../../../types/certificationPreparation';
import type {
  PracticalCandidateProgress,
  PracticalCompleteTaskResult,
  PracticalPaperOption,
  PracticalPaperQuestion,
  PracticalScoreItem,
  PracticalScopeOption,
  PracticalTaskProgressItem,
  PracticalTemplateItem,
} from '../../../types/practicalScoring';

interface Props {
  taskId?: string;
  showSetup?: boolean;
  showTemplate?: boolean;
  showProgress?: boolean;
}

export function PracticalScoringConfig({
  taskId,
  showSetup = true,
  showTemplate = true,
  showProgress = true,
}: Props) {
  const queryClient = useQueryClient();
  const [selectedScopeId, setSelectedScopeId] = useState<string>();
  const [selectedCandidateIds, setSelectedCandidateIds] = useState<string[]>([]);
  const [configModalOpen, setConfigModalOpen] = useState(false);
  const [batchGroupId, setBatchGroupId] = useState<string>();
  const [batchPaperId, setBatchPaperId] = useState<string>();
  const [returningTask, setReturningTask] = useState<PracticalTaskProgressItem>();
  const [returnReason, setReturnReason] = useState('');
  const [adjustingTask, setAdjustingTask] = useState<PracticalTaskProgressItem>();
  const [adjustDeductions, setAdjustDeductions] = useState<Record<string, number>>({});
  const [adjustRemarks, setAdjustRemarks] = useState<Record<string, string>>({});
  const [adjustNote, setAdjustNote] = useState('');
  const [adjustReason, setAdjustReason] = useState('');
  const [selectedSummaryCandidateIds, setSelectedSummaryCandidateIds] = useState<string[]>([]);
  const [editingQuestion, setEditingQuestion] = useState<{
    paper: PracticalPaperOption;
    question: PracticalPaperQuestion;
  }>();
  const [editQuestionTitle, setEditQuestionTitle] = useState('');
  const [editTemplateItems, setEditTemplateItems] = useState<PracticalTemplateItem[]>([]);
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
  const completeSign = progressQuery.data?.completeSign;
  const scoresSettled = totalCandidateCount > 0
    && (progressQuery.data?.candidates || []).every((row) => row.status === 'completed' || row.status === 'absent');
  const signStatus = completeSign?.status || 'none';
  const waitingSign = signStatus === 'pending';
  const allSigned = signStatus === 'signed';
  const completeDisabled = !scoresSettled || waitingSign || signStatus === 'consumed';
  const completeDisabledReason = !scoresSettled
    ? '仅全部考生实操成绩已完成或已标记缺考时可执行'
    : waitingSign
      ? `等待三名考评员签字确认（${completeSign?.signedCount || 0}/${completeSign?.requiredCount || 3}）`
      : signStatus === 'consumed'
        ? '评分任务已完成'
        : undefined;
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
  const selectedCandidateRows = useMemo(
    () => candidateRows.filter((candidate) => selectedCandidateIds.includes(candidate.id)),
    [candidateRows, selectedCandidateIds],
  );
  const resolvedConfigByCandidate = useMemo(() => new Map(candidateRows.map((candidate) => {
    const directConfig = configByCandidate.get(candidate.id);
    if (directConfig) return [candidate.id, directConfig] as const;
    const scope = findScopeForCandidate(setup?.scopes || [], candidate);
    const scopeConfig = scope ? (setup?.configs || []).find((config) => (
      !config.candidateId && String(config.scopeId) === String(scope.id)
    )) : undefined;
    return [candidate.id, scopeConfig] as const;
  })), [candidateRows, configByCandidate, setup?.configs, setup?.scopes]);
  const availableBatchPapers = useMemo(() => (setup?.papers || []).filter((paper) => (
    paper.questions.length > 0
    && paper.questions.every((question) => question.templateReady)
    && selectedCandidateRows.every((candidate) => paperOptionsForCandidate(
      setup?.papers || [],
      candidate,
      findScopeForCandidate(setup?.scopes || [], candidate),
    ).some((option) => option.id === paper.id))
  )), [selectedCandidateRows, setup?.papers, setup?.scopes]);

  const isCandidateUnavailable = (candidate: PreparationCandidate) => (
    Boolean(resolvedConfigByCandidate.get(candidate.id)) || Boolean(progressByCandidate.get(candidate.id)?.totalTasks)
  );

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

  useEffect(() => {
    const availableIds = new Set(candidateRows
      .filter((candidate) => !isCandidateUnavailable(candidate))
      .map((candidate) => candidate.id));
    setSelectedCandidateIds((previous) => previous.filter((candidateId) => availableIds.has(candidateId)));
  }, [candidateRows, progressByCandidate, resolvedConfigByCandidate]);

  useEffect(() => {
    if (batchPaperId && !availableBatchPapers.some((paper) => paper.id === batchPaperId)) {
      setBatchPaperId(undefined);
    }
  }, [availableBatchPapers, batchPaperId]);

  const adjustTotalScore = useMemo(() => {
    const detail = adjustingTaskQuery.data;
    if (!detail) return 0;
    return detail.scoreItems.reduce((sum, item) => (
      sum + Math.max(0, item.maxScore - (adjustDeductions[scoreItemKey(item)] ?? item.deductedScore ?? 0))
    ), 0);
  }, [adjustDeductions, adjustingTaskQuery.data]);

  const batchCandidateLinkMutation = useMutation({
    mutationFn: () => batchLinkPracticalCandidateTemplates(taskId!, {
      candidateIds: selectedCandidateIds,
      paperId: batchPaperId!,
      groupId: batchGroupId!,
    }),
    onSuccess: (result) => {
      message.success(`已为 ${result.configuredCandidates} 名学员生成 ${result.createdTasks} 个评分任务`);
      if (result.skippedCandidates > 0) {
        message.warning(`${result.skippedCandidates} 名已配置或已生成评分任务的学员未处理`);
      }
      setConfigModalOpen(false);
      setSelectedCandidateIds([]);
      setBatchGroupId(undefined);
      setBatchPaperId(undefined);
      refresh();
    },
    onError: (error: unknown) => {
      const reason =
        (error as { response?: { data?: { message?: string } }; message?: string })?.response?.data?.message
        || (error as { message?: string })?.message
        || '批量配置实操试卷失败，请稍后重试';
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
  const editQuestionMutation = useMutation({
    mutationFn: () => savePracticalQuestion(
      editingQuestion!.paper.id,
      editingQuestion!.question.id,
      { title: editQuestionTitle, items: editTemplateItems },
    ),
    onSuccess: () => {
      message.success('评分记录表已修改');
      setEditingQuestion(undefined);
      setEditQuestionTitle('');
      setEditTemplateItems([]);
      refresh();
    },
    onError: (error: unknown) => {
      const reason =
        (error as { response?: { data?: { message?: string } }; message?: string })?.response?.data?.message
        || (error as { message?: string })?.message
        || '修改评分记录表失败，请检查题目名称和评分项配分';
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
  const completeTaskMutation = useMutation({
    mutationFn: () => completePracticalScoringTask(taskId!),
    onSuccess: (result: PracticalCompleteTaskResult) => {
      message.success(`评分任务已完成，已将 ${result.releasedAssessors} 名考评员改为空闲（${result.releasedGroups} 个考评组历史绑定已保留）`);
      refresh();
    },
  });
  const requestCompleteSignMutation = useMutation({
    mutationFn: () => requestPracticalCompleteSign(taskId!),
    onSuccess: (result) => {
      message.success(`已通知考评员签字确认（${result.signedCount}/${result.requiredCount}）`);
      refresh();
    },
  });

  const openQuestionEditor = (paper: PracticalPaperOption, question: PracticalPaperQuestion) => {
    setEditingQuestion({ paper, question });
    setEditQuestionTitle(question.title || '');
    setEditTemplateItems((question.templateItems || []).map((item) => ({ ...item })));
  };

  const updateEditTemplateItem = (itemNo: number, patch: Partial<PracticalTemplateItem>) => {
    setEditTemplateItems((previous) => previous.map((item) => (
      item.itemNo === itemNo ? { ...item, ...patch } : item
    )));
  };

  const addEditTemplateItem = () => {
    const nextItemNo = editTemplateItems.reduce((max, item) => Math.max(max, item.itemNo), 0) + 1;
    setEditTemplateItems((previous) => [
      ...previous,
      {
        itemNo: nextItemNo,
        itemName: `评分要素${nextItemNo}`,
        maxScore: 1,
        criterion: '',
        maxDeduction: 0,
        deductionStep: 1,
      },
    ]);
  };

  const editTemplateTotal = editTemplateItems.reduce((sum, item) => sum + Number(item.maxScore || 0), 0);
  const editTargetScore = editingQuestion?.question.score;
  const editScoreMatches = editTargetScore == null
    || Math.abs(editTemplateTotal - Number(editTargetScore)) < 0.0001;

  if (!taskId) return <Typography.Text type="secondary">请选择认定任务。</Typography.Text>;

  const candidateColumns: ColumnsType<PreparationCandidate> = [
    { title: '学员', dataIndex: 'candidateName', width: 120, fixed: 'left' },
    { title: '证件号', dataIndex: 'maskedIdCardNo', width: 180, render: (v, row) => v || row.idCardNo || '-' },
    { title: '手机号', dataIndex: 'maskedPhone', width: 140, render: (v, row) => v || row.phone || '-' },
    { title: '工种', dataIndex: 'workTypeName', width: 120, render: (v) => v || '-' },
    { title: '等级', dataIndex: 'levelName', width: 100, render: (v, row) => v || row.levelCode || '-' },
    { title: '单位', dataIndex: 'orgName', width: 160, ellipsis: true, render: (v) => v || '-' },
    {
      title: '已绑定考评组',
      width: 180,
      render: (_, row) => {
        const config = resolvedConfigByCandidate.get(row.id);
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
        const configured = resolvedConfigByCandidate.get(row.id);
        const progress = progressByCandidate.get(row.id);
        if (progress?.totalTasks) return <Tag color="success">已生成评分任务</Tag>;
        if (configured) return <Tag color="processing">已配置</Tag>;
        return <Tag>未配置</Tag>;
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

  const paperColumns: ColumnsType<PracticalPaperOption> = [
    {
      title: '评分记录表',
      dataIndex: 'title',
      ellipsis: true,
      render: (value) => value || '未命名评分记录表',
    },
    {
      title: '试题数',
      width: 90,
      render: (_, paper) => paper.questions.length,
    },
    {
      title: '评分模板',
      width: 120,
      render: (_, paper) => {
        const ready = paper.questions.filter((question) => question.templateReady).length;
        return <Tag color={ready === paper.questions.length && ready > 0 ? 'success' : 'warning'}>{ready}/{paper.questions.length} 已配置</Tag>;
      },
    },
  ];

  const questionColumnsForPaper = (paper: PracticalPaperOption): ColumnsType<PracticalPaperQuestion> => [
    {
      title: '题目名称',
      dataIndex: 'title',
      ellipsis: true,
      render: (value) => value || <Typography.Text type="danger">未命名题目</Typography.Text>,
    },
    { title: '题目分值', dataIndex: 'score', width: 100, render: (value) => value ?? '-' },
    {
      title: '评分项',
      width: 100,
      render: (_, question) => `${question.templateItems?.length || 0} 项`,
    },
    {
      title: '操作',
      width: 100,
      render: (_, question) => (
        <Button
          type="link"
          size="small"
          icon={<EditOutlined />}
          onClick={() => openQuestionEditor(paper, question)}
        >
          修改
        </Button>
      ),
    },
  ];

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      {showConfig && (
        <ProCard
          title="学员列表"
          extra={<Button icon={<ReloadOutlined />} onClick={refresh}>刷新</Button>}
        >
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
              <Button
                type="primary"
                disabled={selectedCandidateIds.length === 0}
                onClick={() => {
                  setBatchGroupId(undefined);
                  setBatchPaperId(undefined);
                  setConfigModalOpen(true);
                }}
              >
                配置{selectedCandidateIds.length ? `（${selectedCandidateIds.length}）` : ''}
              </Button>
            </Space>
            <Table
              rowKey="id"
              columns={candidateColumns}
              dataSource={candidateRows}
              loading={candidateQuery.isFetching || setupQuery.isFetching || progressQuery.isFetching}
              scroll={{ x: 1000 }}
              rowSelection={{
                selectedRowKeys: selectedCandidateIds,
                onChange: (keys) => setSelectedCandidateIds(keys.map(String)),
                getCheckboxProps: (row) => ({
                  disabled: isCandidateUnavailable(row),
                }),
              }}
              pagination={{ pageSize: 10, showSizeChanger: false }}
              locale={{
                emptyText: selectedScopeId ? '当前工种等级下暂无已完成申报材料的学员' : '暂无已完成申报材料的学员',
              }}
            />
          </Space>
        </ProCard>
      )}

      {showTemplate && (
        <ProCard title="评分记录表配置">
          <Alert
            type="info"
            showIcon
            message="导入后可直接修改题目名称和评分明细"
            description="题目名称不能为空，评分项配分合计必须等于题目分值。尚未开始评分的任务会同步更新；已开始评分的任务需先处理后再修改。"
            style={{ marginBottom: 16 }}
          />
          <Table<PracticalPaperOption>
            rowKey="id"
            columns={paperColumns}
            dataSource={setup?.papers || []}
            loading={setupQuery.isFetching}
            pagination={{ pageSize: 8, showSizeChanger: false }}
            expandable={{
              expandedRowRender: (paper) => (
                <Table<PracticalPaperQuestion>
                  rowKey="id"
                  columns={questionColumnsForPaper(paper)}
                  dataSource={paper.questions}
                  pagination={false}
                  size="small"
                  locale={{ emptyText: '该评分记录表暂无题目' }}
                />
              ),
              rowExpandable: (paper) => paper.questions.length > 0,
            }}
            locale={{ emptyText: '暂无可修改的实操评分记录表，请先导入评分记录表' }}
          />
        </ProCard>
      )}

      <Modal
        title={`批量配置实操考试（已选 ${selectedCandidateIds.length} 名学员）`}
        open={configModalOpen}
        okText="确认配置"
        cancelText="取消"
        confirmLoading={batchCandidateLinkMutation.isPending}
        onCancel={() => setConfigModalOpen(false)}
        onOk={() => {
          if (!batchGroupId) {
            message.warning('请选择考评组');
            return;
          }
          if (!batchPaperId) {
            message.warning('请选择实操试卷');
            return;
          }
          batchCandidateLinkMutation.mutate();
        }}
      >
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <Typography.Text type="secondary">
            已配置或已生成评分任务的学员不可勾选；本次将为所选学员统一绑定以下考评组和试卷。
          </Typography.Text>
          <div>
            <Typography.Text strong>考评组</Typography.Text>
            <Select
              showSearch
              optionFilterProp="label"
              value={batchGroupId}
              placeholder="请选择满足三人成组条件的考评组"
              style={{ width: '100%', marginTop: 8 }}
              options={(groupsQuery.data || []).map((group) => {
                const count = resolveMemberCount(group.id);
                return {
                  value: group.id,
                  disabled: count !== 3,
                  label: `${group.groupName}（${count}/3人${group.leaderAssessorName ? ` · 组长${group.leaderAssessorName}` : ''}）`,
                };
              })}
              onChange={setBatchGroupId}
            />
          </div>
          <div>
            <Typography.Text strong>实操试卷</Typography.Text>
            <Select
              showSearch
              optionFilterProp="label"
              value={batchPaperId}
              placeholder="请选择可用于所有已选学员的实操试卷"
              style={{ width: '100%', marginTop: 8 }}
              options={availableBatchPapers.map((paper) => ({ value: paper.id, label: paper.title }))}
              notFoundContent="没有可统一用于所选学员的已完成模板试卷"
              onChange={setBatchPaperId}
            />
          </div>
        </Space>
      </Modal>

      <Modal
        title={`修改评分记录表${editingQuestion?.question.title ? ` · ${editingQuestion.question.title}` : ''}`}
        open={Boolean(editingQuestion)}
        width={1120}
        okText="保存修改"
        cancelText="取消"
        confirmLoading={editQuestionMutation.isPending}
        okButtonProps={{
          disabled: !editQuestionTitle.trim() || editTemplateItems.length === 0 || !editScoreMatches,
        }}
        onCancel={() => {
          setEditingQuestion(undefined);
          setEditQuestionTitle('');
          setEditTemplateItems([]);
        }}
        onOk={() => editQuestionMutation.mutate()}
      >
        <Space direction="vertical" size={14} style={{ width: '100%' }}>
          <div>
            <Typography.Text strong>题目名称</Typography.Text>
            <Input
              value={editQuestionTitle}
              maxLength={120}
              showCount
              placeholder="请输入题目名称"
              style={{ marginTop: 8 }}
              onChange={(event) => setEditQuestionTitle(event.target.value)}
            />
          </div>
          <Space>
            <Typography.Text strong>评分项明细</Typography.Text>
            <Typography.Text type={
              editingQuestion && Math.abs(editTemplateTotal - Number(editingQuestion.question.score || 0)) < 0.0001
                ? 'success'
                : 'danger'
            }>
              配分合计 {editTemplateTotal} / 题目分值 {editingQuestion?.question.score ?? '-'}
            </Typography.Text>
          </Space>
          <Table<PracticalTemplateItem>
            rowKey="itemNo"
            size="small"
            pagination={false}
            scroll={{ x: 950 }}
            dataSource={editTemplateItems}
            columns={[
              { title: '序号', dataIndex: 'itemNo', width: 70 },
              {
                title: '评分要素',
                width: 180,
                render: (_, item) => (
                  <Input
                    value={item.itemName}
                    onChange={(event) => updateEditTemplateItem(item.itemNo, { itemName: event.target.value })}
                  />
                ),
              },
              {
                title: '配分',
                width: 110,
                render: (_, item) => (
                  <InputNumber
                    min={0.01}
                    precision={2}
                    style={{ width: '100%' }}
                    value={item.maxScore}
                    onChange={(value) => updateEditTemplateItem(item.itemNo, { maxScore: Number(value || 0) })}
                  />
                ),
              },
              {
                title: '评分标准',
                width: 300,
                render: (_, item) => (
                  <Input
                    value={item.criterion}
                    onChange={(event) => updateEditTemplateItem(item.itemNo, { criterion: event.target.value })}
                  />
                ),
              },
              {
                title: '最大扣分',
                width: 110,
                render: (_, item) => (
                  <InputNumber
                    min={0}
                    precision={2}
                    style={{ width: '100%' }}
                    value={item.maxDeduction}
                    onChange={(value) => updateEditTemplateItem(item.itemNo, { maxDeduction: Number(value || 0) })}
                  />
                ),
              },
              {
                title: '步长',
                width: 100,
                render: (_, item) => (
                  <InputNumber
                    min={0.01}
                    precision={2}
                    style={{ width: '100%' }}
                    value={item.deductionStep}
                    onChange={(value) => updateEditTemplateItem(item.itemNo, { deductionStep: Number(value || 0) })}
                  />
                ),
              },
              {
                title: '操作',
                width: 70,
                render: (_, item) => (
                  <Button
                    type="link"
                    danger
                    icon={<DeleteOutlined />}
                    onClick={() => setEditTemplateItems((previous) => previous.filter((row) => row.itemNo !== item.itemNo))}
                  />
                ),
              },
            ]}
          />
          <Button type="dashed" icon={<PlusOutlined />} onClick={addEditTemplateItem}>
            新增评分项
          </Button>
        </Space>
      </Modal>

      {showProgress && (
        <ProCard title="评分进度">
          <Space wrap style={{ marginBottom: 16 }}>
            <Tooltip title={completeDisabled ? completeDisabledReason : undefined}>
              <span>
                <Button
                  type="primary"
                  danger
                  disabled={completeDisabled}
                  loading={completeTaskMutation.isPending || requestCompleteSignMutation.isPending}
                  onClick={() => {
                    if (!allSigned) {
                      Modal.confirm({
                        title: '确认完成评分任务？',
                        content: (
                          <Space direction="vertical" size={8}>
                            <Typography.Paragraph style={{ marginBottom: 0 }}>
                              仅全部考生实操成绩已完成或已标记缺考时可执行。完成后会把本任务考评员从「认定任务中」改为「空闲」，考评组绑定和历史评分记录会保留。
                            </Typography.Paragraph>
                            <Typography.Paragraph strong style={{ marginBottom: 0 }}>
                              三名考评员签字确认后，完成任务按钮才会生效。
                            </Typography.Paragraph>
                          </Space>
                        ),
                        okText: '通知考评员签字',
                        cancelText: '取消',
                        onOk: () => requestCompleteSignMutation.mutateAsync(),
                      });
                      return;
                    }
                    Modal.confirm({
                      title: '确认完成评分任务？',
                      content: '三名考评员已签字确认。完成后会把本任务考评员从「认定任务中」改为「空闲」，考评组绑定和历史评分记录会保留，签名将写入评分记录表。',
                      okText: '完成任务并释放考评员',
                      cancelText: '取消',
                      okButtonProps: { danger: true },
                      onOk: () => completeTaskMutation.mutateAsync(),
                    });
                  }}
                >
                  完成任务
                </Button>
              </span>
            </Tooltip>
            <Button
              icon={<DownloadOutlined />}
              disabled={selectedSummaryCandidateIds.length === 0}
              loading={exportSummaryMutation.isPending}
              onClick={() => exportSummaryMutation.mutate()}
            >
              导出实操评分表
            </Button>
          </Space>
          {waitingSign && (
            <Alert
              type="warning"
              showIcon
              style={{ marginBottom: 16 }}
              message="三名考评员签字确认"
              description={`已通知实操评分端弹出电子签名。当前 ${completeSign?.signedCount || 0}/${completeSign?.requiredCount || 3} 名考评员已签字，全部签字完成后“完成任务”按钮将可用。`}
            />
          )}
          {allSigned && (
            <Alert
              type="success"
              showIcon
              style={{ marginBottom: 16 }}
              message="三名考评员已签字确认"
              description="现在可以点击“完成任务”，把考评员改为空闲并保留历史绑定。"
            />
          )}
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
